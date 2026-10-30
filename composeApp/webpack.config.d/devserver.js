// The web dev server runs beside the Quarkus dev server (port 8080), on the port the server's dev
// profile allows for CORS (`%dev.quarkus.http.cors.origins` in server/src/main/resources/application.properties).
if (config.devServer) {
    config.devServer.port = 8081;
}
