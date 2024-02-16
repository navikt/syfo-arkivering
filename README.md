spinnsyn-arkivering
================

Arkiverer vedtak i joark slik de er vist i visningsløsningen for sykepenger.

# Komme i gang

Bygges med `gradle clean build` som et helt vanlig Spring Boot prosjekt.

---

# Hvordan teste PDF-generering lokalt

Kompiler `spinnsyn-frontend` med `npm run build`.

Start frontend med `npm run start-ingen-dekorator`

Start applikasjonen i `src/test/kotlin/no/helse/flex/localtesting/Application.kt` i IntelliJ. Husk å angi
at `Active profile` er `localtesting` i `Run/Debug Configurations`.

Åpne `http://localhost:8888/api/test/pdf/` og du får lastet ned en PDF

## Data

Applikasjonen har en database i GCP.

Tabellen `arkivert_vedtak` holder oversikt over vedtak vi har arkivert. Tabellen inkluderer `fødselsnummer`, `vedtak_id`
og `journalpost_id` og er derfor personidentifiserbar. 

Det slettes ikke data fra tabellen.

# Henvendelser

- Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.
- Spørsmål knyttet til koden eller prosjektet kan stilles til flex@nav.no

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen `#flex`.
