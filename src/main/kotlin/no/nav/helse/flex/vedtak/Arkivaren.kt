package no.nav.helse.flex.vedtak

import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.html.HtmlInliner
import no.nav.helse.flex.pdfgenerering.PdfGenerering.createPDFA
import org.springframework.stereotype.Component

@Component
class Arkivaren(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient,
    val htmlInliner: HtmlInliner,
) {
    fun hentSomHtmlOgInlineTing(fnr: String, utbetalingId: String): String {
        val html = spinnsynFrontendArkiveringClient.hentVedtakSomHtml(utbetalingId = utbetalingId, fnr = fnr)
        return htmlInliner.inlineHtml(html)
    }

    fun hentPdf(fnr: String, utbetalingId: String): ByteArray {

        val html = hentSomHtmlOgInlineTing(fnr, utbetalingId)

        return hentPdfFraHTml(html)
    }

    fun hentPdfFraHTml(html: String): ByteArray {
        val nyDoctype = """
            <!DOCTYPE html PUBLIC
 "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN" "">
        """.trimIndent()

        return createPDFA(html.replaceFirst("<!DOCTYPE html>", nyDoctype))
    }
}
