env "local" {
  url = "postgres://postgres:postgres@localhost:5432/greenprint?sslmode=disable"
  dev = "docker://postgres/16/dev"
  migration {
    dir = "file://migrations"
  }
}
