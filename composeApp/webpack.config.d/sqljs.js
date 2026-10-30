// The web cache (ADR-017) runs sql.js in a web worker. webpack picks sql.js's browser build, which
// loads its wasm file from next to the worker, so the wasm file is copied into the bundle.
const CopyWebpackPlugin = require("copy-webpack-plugin");
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [require.resolve("sql.js/dist/sql-wasm-browser.wasm")],
    }),
);
