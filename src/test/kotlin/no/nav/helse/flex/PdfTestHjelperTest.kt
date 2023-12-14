package no.nav.helse.flex

import no.nav.helse.flex.arkivering.PdfSkaperen
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.OffsetDateTime

class PdfTestHjelperTest : Testoppsett() {
    @Autowired
    lateinit var arkivaren: PdfSkaperen

    @Test
    fun pdfHjelper() {
        /* For å lokalt kjøre pdf genereringa av html hentet fra testmiljøet
         * https://spinnsyn-arkivering.dev.nav.no/api/test/html/{fnr}/{utbetalingId}
         * */

        val file = File("pdf-tests/test.html")
        if (file.exists()) {
            val pdf = arkivaren.hentPdfFraHtml(file.readText())
            File("pdf-tests/" + OffsetDateTime.now().toString() + ".pdf").writeBytes(pdf)
        }
    }
}
