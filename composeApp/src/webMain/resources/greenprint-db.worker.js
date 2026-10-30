// The web app's local cache (ADR-017): SQLite from sql.js, run in a web worker that speaks the
// protocol of SQLDelight's WebWorkerDriver. sql.js keeps the database in memory, so this worker saves
// a copy to IndexedDB after every committed write and opens that copy on the next start.
import initSqlJs from "sql.js";

const IDB_NAME = "greenprint";
const IDB_STORE = "databases";
const IDB_KEY = "greenprint.db";
const SAVE_DELAY_MS = 100;
const WRITE = /^\s*(INSERT|UPDATE|DELETE|REPLACE|CREATE|DROP|ALTER|PRAGMA\s+user_version\s*=)/i;

let db = null;
let inTransaction = false;
let dirty = false;
let saveTimer = null;

function openStore() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(IDB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(IDB_STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function loadSnapshot() {
  const store = await openStore();
  return new Promise((resolve, reject) => {
    const request = store.transaction(IDB_STORE, "readonly").objectStore(IDB_STORE).get(IDB_KEY);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function saveSnapshot(bytes) {
  const store = await openStore();
  return new Promise((resolve, reject) => {
    const transaction = store.transaction(IDB_STORE, "readwrite");
    transaction.objectStore(IDB_STORE).put(bytes, IDB_KEY);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });
}

async function openDatabase() {
  // composeApp/webpack.config.d/sqljs.js copies the wasm file (sql-wasm-browser.wasm) next to this worker.
  const SQL = await initSqlJs({ locateFile: (file) => new URL(file, self.location.href).href });
  let snapshot;
  try {
    snapshot = await loadSnapshot();
  } catch (error) {
    // No IndexedDB (a private window may refuse it): the cache still works, for this visit only.
    console.warn("GreenPrint: the local cache is not saved in this browser", error);
  }
  db = snapshot ? new SQL.Database(snapshot) : new SQL.Database();
}

// sql.js closes and reopens the database to export it, so a snapshot is only taken between
// transactions; a write inside a transaction is saved when it commits.
function scheduleSave() {
  dirty = true;
  if (inTransaction) return;
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    if (!dirty || inTransaction) return;
    dirty = false;
    saveSnapshot(db.export()).catch((error) => console.warn("GreenPrint: saving the local cache failed", error));
  }, SAVE_DELAY_MS);
}

function handle(data) {
  switch (data && data.action) {
    case "exec": {
      if (!data.sql) throw new Error("exec: Missing query string");
      const results = db.exec(data.sql, data.params)[0] ?? { values: [] };
      if (WRITE.test(data.sql)) scheduleSave();
      return results;
    }
    case "begin_transaction":
      inTransaction = true;
      return db.exec("BEGIN TRANSACTION;");
    case "end_transaction": {
      const results = db.exec("END TRANSACTION;");
      inTransaction = false;
      if (dirty) scheduleSave();
      return results;
    }
    case "rollback_transaction": {
      const results = db.exec("ROLLBACK TRANSACTION;");
      inTransaction = false;
      // A write made before the transaction may still be waiting for its save.
      if (dirty) scheduleSave();
      return results;
    }
    default:
      throw new Error(`Unsupported action: ${data && data.action}`);
  }
}

const ready = openDatabase();
self.onmessage = (event) =>
  ready
    .then(() => postMessage({ id: event.data.id, results: handle(event.data) }))
    .catch((error) => postMessage({ id: event.data.id, error: { name: error.name, message: error.message } }));
