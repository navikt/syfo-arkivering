package no.nav.helse.flex.arkivering

import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.html.HtmlInliner
import no.nav.helse.flex.pdfgenerering.PdfGenerering.createPDFA
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PdfSkaperen(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient,
    val htmlInliner: HtmlInliner,
) {
    data class PdfVedtak(
        val pdf: ByteArray,
        val versjon: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    fun hentPdf(
        fnr: String,
        id: String,
    ): PdfVedtak {
        val html = hentSomHtmlOgInlineTing(fnr = fnr, id = id)

        val pdf = hentPdfFraHtml(html.html)
        return PdfVedtak(
            pdf = pdf,
            versjon = html.versjon,
            fom = html.fom,
            tom = html.tom,
        )
    }

    fun hentSomHtmlOgInlineTing(
        fnr: String,
        id: String,
    ): SpinnsynFrontendArkiveringClient.HtmlVedtak {
        val htmlVedtak = spinnsynFrontendArkiveringClient.hentVedtakSomHtml(fnr = fnr, id = id)
        return htmlVedtak.copy(html = htmlInliner.inlineHtml(htmlVedtak.html, utbetalingId = id, fnr = fnr))
    }

    fun hentPdfFraHtml(html: String): ByteArray {
        val nyDoctype =
            """
                       <!DOCTYPE html PUBLIC
            "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN" "">
            """.trimIndent()

        return createPDFA(html.replaceFirst("<!DOCTYPE html>", nyDoctype))
    }
}
