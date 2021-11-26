package no.nav.helse.flex

import no.nav.helse.flex.arkivering.Arkivaren
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.OffsetDateTime
import java.util.*

class HentingOgPdfGenereringTest : Testoppsett() {

    @Autowired
    lateinit var arkivaren: Arkivaren

    val uuid = UUID.randomUUID().toString()
    val fnr = "13068712345"

    @Test
    fun henterHtml() {
        enqueFiler()

        val html = arkivaren.hentSomHtmlOgInlineTing(fnr, uuid)
        val forventetHtml = HentingOgPdfGenereringTest::class.java.getResource("/forventet.html").readText()
        html.html `should be equal to ignoring whitespace` forventetHtml

        validerRequests(uuid, fnr)
    }

    @Test
    fun henterPdf() {
        enqueFiler()

        File("pdf-tests").mkdirs()
        val pdf = arkivaren.hentPdf(fnr, uuid)
        File("pdf-tests/" + OffsetDateTime.now().toString() + ".pdf").writeBytes(pdf.pdf)

        validerRequests(uuid, fnr)
    }
}
