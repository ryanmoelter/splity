# Generating the API

The generation plugin isn't perfect, so here's what to do:

1. `gradle :ynab-api:generateSwagger`
2. Replace all `&#x60;` instances with \`
3. Add `@Json(name = "") NONE("")` under all `PURPLE("purple")` enum values
4. Make everything compile against all the new changes 🙃
