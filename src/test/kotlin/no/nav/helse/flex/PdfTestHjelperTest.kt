package no.nav.helse.flex

import no.nav.helse.flex.vedtak.Arkivaren
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.OffsetDateTime
import java.util.*

class PdfTestHjelperTest : Testoppsett() {

    @Autowired
    lateinit var arkivaren: Arkivaren

    @Test
    fun pdfHjelper() {
        /* For å lokalt kjøre pdf genereringa av html hentet fra testmiljøet
        * https://spinnsyn-arkivering.dev.nav.no/api/test/html/{fnr}/{utbetalingId}
        * */

        val file = File("pdf-tests/test.html")
        if (file.exists()) {
            val pdf = arkivaren.hentPdfFraHTml(file.readText())
            File("pdf-tests/" + OffsetDateTime.now().toString() + ".pdf").writeBytes(pdf)
        }
    }
}
