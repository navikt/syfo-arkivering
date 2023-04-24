package no.nav.helse.flex

import no.nav.helse.flex.arkivering.PdfSkaperen
import no.nav.helse.flex.html.HtmlInliner
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class HentingOgPdfGenereringTest : Testoppsett() {

    @Autowired
    lateinit var arkivaren: PdfSkaperen

    @Autowired
    lateinit var htmlInliner: HtmlInliner

    val uuid = "331c9cf7-b2b4-4c60-938d-00ac3969666b"
    val fnr = "13068712345"

    @Test
    fun `henter html som funker`() {
        htmlInliner.clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("Europe/Oslo"))
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

    @Test
    fun `sjekker at vi ikke støtter flex`() {
        enqueFil(
            "/flexstyling.html",
            imageName = "docker.github/spinnsyn-frontend-v2.0",
            fom = "2020-03-12",
            tom = "2020-04-30"
        )

        val ex = assertThrows(RuntimeException::class.java) {
            arkivaren.hentSomHtmlOgInlineTing(fnr, uuid)
        }

        ex.message `should be equal to` "Flex-styling er ikke støttet"

        val htmlRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        htmlRequest.path `should be equal to` "/syk/sykepenger/vedtak/arkivering/$uuid"
    }

    @Test
    fun `sjekker at bodys child må være __next`() {
        enqueFil(
            "/ingen__next.html",
            imageName = "docker.github/spinnsyn-frontend-v2.0",
            fom = "2020-03-12",
            tom = "2020-04-30"
        )

        val ex = assertThrows(RuntimeException::class.java) {
            arkivaren.hentSomHtmlOgInlineTing(fnr, uuid)
        }

        ex.message `should be equal to` "Forventa at første child har id __next"

        val htmlRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        htmlRequest.path `should be equal to` "/syk/sykepenger/vedtak/arkivering/$uuid"
    }

    @Test
    fun `sjekker at bodys kan ha en child`() {
        enqueFil(
            "/flere_child_til_body.html",
            imageName = "docker.github/spinnsyn-frontend-v2.0",
            fom = "2020-03-12",
            tom = "2020-04-30"
        )

        val ex = assertThrows(RuntimeException::class.java) {
            arkivaren.hentSomHtmlOgInlineTing(fnr, uuid)
        }

        ex.message `should be equal to` "Forventa bare en child til body"

        val htmlRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        htmlRequest.path `should be equal to` "/syk/sykepenger/vedtak/arkivering/$uuid"
    }
}
