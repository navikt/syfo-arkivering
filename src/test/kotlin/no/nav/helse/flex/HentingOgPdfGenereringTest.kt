package no.nav.helse.flex

import no.nav.helse.flex.vedtak.Arkivaren
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldStartWith
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
        enqueDetSomTrengs()

        val html = arkivaren.hentSomHtmlOgInlineTing(fnr, uuid)
        val forventetHtml = HentingOgPdfGenereringTest::class.java.getResource("/forventet.html").readText()
        html `should be equal to ignoring whitespace` forventetHtml

        validerRequests()
    }

    @Test
    fun henterPdf() {
        enqueDetSomTrengs()

        File("pdf-tests").mkdirs()
        val pdf = arkivaren.hentPdf(fnr, uuid)
        File("pdf-tests/" + OffsetDateTime.now().toString() + ".pdf").writeBytes(pdf)

        validerRequests()
    }

    fun validerRequests() {
        val htmlRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        htmlRequest.path `should be equal to` "/syk/sykepenger/vedtak/arkivering/$uuid"
        htmlRequest.headers["fnr"] `should be equal to` fnr
        htmlRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")

        val stylesheetRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        stylesheetRequest.path `should be equal to` "/syk/sykepenger/_next/static/css/yes.css"
        stylesheetRequest.headers["fnr"].shouldBeNull()
        stylesheetRequest.headers["Authorization"].shouldBeNull()

        val svgRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        svgRequest.path `should be equal to` "/public/ikon-skriv-til-oss.svg"
        svgRequest.headers["fnr"].shouldBeNull()
        svgRequest.headers["Authorization"].shouldBeNull()
    }

    fun enqueDetSomTrengs() {
        enqueFil("/testside.html")
        enqueFil("/stylesheet.css")
        enqueFil("/ikon-skriv-til-oss.svg")
    }

    fun enqueFil(fil: String) {
        val innhold = HentingOgPdfGenereringTest::class.java.getResource(fil).readText()

        spinnsynArkiveringFrontendMockWebServer.enqueue(MockResponse().setBody(innhold))
    }
}
