spinnsyn-arkivering
================

Arkiverer vedtak slik de er vist i visningsløsningen for sykepenger i joark

# Komme i gang

Bygges med gradle som et helt vanlig spring boot prosjekt

---

# Hvordan teste pdf generering lokalt
Kompiler `spinnsyn-frontend` med `npm run build`. 

Start frontend med `npm run start-ingen-dekorator`

Start applikasjonen i `src/test/kotlin/no/helse/flex/localtesting/Application.kt` i intellij

Åpne `http://localhost:8888/api/test/pdf/` og du får lastet ned en PDF 

## Data
Applikasjonen har en database i GCP.

Tabellen `ARKIVERT_VEDTAK` holder oversikt over vedtak vi har arkivert.
Tabellen inkluderer fødselsnummer, vedtak_id og journalpost_id og er derfor personidentifiserbar. Det slettes ikke data fra tabellen.


# Henvendelser

Enten:
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

Eller:
Spørsmål knyttet til koden eller prosjektet kan stilles til flex@nav.no

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #flex.
