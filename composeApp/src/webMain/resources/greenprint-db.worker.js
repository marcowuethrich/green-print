// The web app's local cache (ADR-017): SQLite from sql.js, run in a web worker that speaks the
// protocol of SQLDelight's WebWorkerDriver. sql.js keeps the database in memory, so this worker saves
// a copy to IndexedDB at once after every committed write (no delay a closing tab could cut short)
// and opens that copy on the next start. A copy that does not open is logged and discarded, and the
// cache starts empty. The page may post { action: "open", discardSaved: true } first to start empty
// on purpose, after the saved copy broke the app's start (Main.kt).
import initSqlJs from "sql.js";

const IDB_NAME = "greenprint";
const IDB_STORE = "databases";
const IDB_KEY = "greenprint.db";
const WRITE = /^\s*(INSERT|UPDATE|DELETE|REPLACE|CREATE|DROP|ALTER|PRAGMA\s+user_version\s*=)/i;

let db = null;
let inTransaction = false;
let dirty = false;
let store = null;

// One connection for the worker's life; a failed open is tried again on the next save.
function openStore() {
  store ??= new Promise((resolve, reject) => {
    const request = indexedDB.open(IDB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(IDB_STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => {
      store = null;
      reject(request.error);
    };
  });
  return store;
}

async function loadSnapshot() {
  const connection = await openStore();
  return new Promise((resolve, reject) => {
    const request = connection.transaction(IDB_STORE, "readonly").objectStore(IDB_STORE).get(IDB_KEY);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

// Writes the snapshot, or deletes it when bytes is null. IndexedDB runs the writes of one store in the
// order they were made, so the last save wins.
async function writeSnapshot(bytes) {
  const connection = await openStore();
  return new Promise((resolve, reject) => {
    const transaction = connection.transaction(IDB_STORE, "readwrite");
    const objects = transaction.objectStore(IDB_STORE);
    if (bytes) objects.put(bytes, IDB_KEY);
    else objects.delete(IDB_KEY);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
    transaction.onabort = () => reject(transaction.error);
  });
}

function discardSnapshot(reason, error) {
  console.error(`GreenPrint: ${reason}; the local cache starts empty`, error);
  writeSnapshot(null).catch((deleteError) => console.warn("GreenPrint: deleting the local cache failed", deleteError));
}

// sql.js may read a damaged file without complaint and fail on the first query, so the copy is checked.
function openSnapshot(SQL, snapshot) {
  let candidate = null;
  try {
    candidate = new SQL.Database(snapshot);
    const check = candidate.exec("PRAGMA quick_check;")[0]?.values?.[0]?.[0];
    if (check === "ok") return candidate;
    throw new Error(`quick_check: ${check}`);
  } catch (error) {
    candidate?.close();
    discardSnapshot("the saved local cache does not open", error);
    return new SQL.Database();
  }
}

async function openDatabase(discardSaved) {
  // composeApp/webpack.config.d/sqljs.js copies the wasm file (sql-wasm-browser.wasm) next to this worker.
  const SQL = await initSqlJs({ locateFile: (file) => new URL(file, self.location.href).href });
  if (discardSaved) {
    discardSnapshot("the app could not start on the saved local cache");
    db = new SQL.Database();
    return;
  }
  let snapshot;
  try {
    snapshot = await loadSnapshot();
  } catch (error) {
    // No IndexedDB (a private window may refuse it): the cache still works, for this visit only.
    console.warn("GreenPrint: the local cache is not saved in this browser", error);
  }
  db = snapshot ? openSnapshot(SQL, snapshot) : new SQL.Database();
}

// sql.js closes and reopens the database to export it, so a snapshot is only taken between
// transactions; a write inside a transaction is saved when it commits.
function save() {
  dirty = true;
  if (inTransaction) return;
  dirty = false;
  writeSnapshot(db.export()).catch((error) => console.warn("GreenPrint: saving the local cache failed", error));
}

function handle(data) {
  switch (data && data.action) {
    case "exec": {
      if (!data.sql) throw new Error("exec: Missing query string");
      const results = db.exec(data.sql, data.params)[0] ?? { values: [] };
      if (WRITE.test(data.sql)) save();
      return results;
    }
    case "begin_transaction":
      inTransaction = true;
      return db.exec("BEGIN TRANSACTION;");
    case "end_transaction": {
      const results = db.exec("END TRANSACTION;");
      inTransaction = false;
      if (dirty) save();
      return results;
    }
    case "rollback_transaction": {
      const results = db.exec("ROLLBACK TRANSACTION;");
      inTransaction = false;
      // A write made before the transaction may still be waiting for its save.
      if (dirty) save();
      return results;
    }
    default:
      throw new Error(`Unsupported action: ${data && data.action}`);
  }
}

let ready = null;
self.onmessage = (event) => {
  if (event.data && event.data.action === "open") {
    ready ??= openDatabase(event.data.discardSaved === true);
    return;
  }
  ready ??= openDatabase(false);
  ready
    .then(() => postMessage({ id: event.data.id, results: handle(event.data) }))
    .catch((error) => postMessage({ id: event.data.id, error: { name: error.name, message: error.message } }));
};
