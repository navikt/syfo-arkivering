package no.nav.helse.flex.localtesting

import jakarta.servlet.http.HttpServletResponse
import no.nav.helse.flex.arkivering.PdfSkaperen
import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.html.HtmlInliner
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider

@SpringBootApplication(
    exclude = [DataSourceAutoConfiguration::class]
)
class Application

fun main(args: Array<String>) {
    VeraGreenfieldFoundryProvider.initialise()
    runApplication<Application>(*args)
}

@Controller
@Unprotected
class TestController() {

    final val pdfSkaperen: PdfSkaperen

    init {
        val url = "http://localhost:8080"

        val htmlInliner = HtmlInliner(url)
        val spinnsynFrontendArkiveringClient = SpinnsynFrontendArkiveringClient(
            spinnsynFrontendArkiveringRestTemplate = RestTemplateBuilder().build(),
            url = url
        )
        pdfSkaperen = PdfSkaperen(spinnsynFrontendArkiveringClient, htmlInliner)
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHtml(response: HttpServletResponse): String {
        val hentSomHtmlOgInlineTing = pdfSkaperen.hentSomHtmlOgInlineTing(fnr = "whatever", id = "utvikling-arkivering")
        return hentSomHtmlOgInlineTing.html
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/pdf/"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(
        response: HttpServletResponse
    ): ByteArray {
        val hentPdf = pdfSkaperen.hentPdf(fnr = "whatever", id = "utvikling-arkivering")
        response.setHeader("x-nais-app-image", hentPdf.versjon)

        return hentPdf.pdf
    }
}
