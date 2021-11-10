package no.nav.helse.flex.arkivering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.html.HtmlInliner
import no.nav.helse.flex.kafka.VedtakStatus
import no.nav.helse.flex.logger
import no.nav.helse.flex.pdfgenerering.PdfGenerering.createPDFA
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class Arkivaren(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient,
    val htmlInliner: HtmlInliner,
    val dokArkivClient: DokArkivClient,
    val environmentToggles: EnvironmentToggles,
    val arkivertVedtakRepository: ArkivertVedtakRepository,
    val registry: MeterRegistry,
    @Value("\${nais.app.image}")
    val naisAppImage: String
) {
    fun hentSomHtmlOgInlineTing(fnr: String, utbetalingId: String): Pair<String, String> {
        val html = spinnsynFrontendArkiveringClient.hentVedtakSomHtml(utbetalingId = utbetalingId, fnr = fnr)
        return Pair(htmlInliner.inlineHtml(html.html), html.versjon)
    }

    fun hentPdf(fnr: String, utbetalingId: String): Pair<ByteArray, String> {

        val html = hentSomHtmlOgInlineTing(fnr, utbetalingId)

        return Pair(hentPdfFraHtml(html.first), html.second)
    }

    fun hentPdfFraHtml(html: String): ByteArray {
        val nyDoctype = """
            <!DOCTYPE html PUBLIC
 "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN" "">
        """.trimIndent()

        return createPDFA(html.replaceFirst("<!DOCTYPE html>", nyDoctype))
    }

    fun arkiverVedtak(vedtak: VedtakStatus): Int {
        val log = logger()

        if (arkivertVedtakRepository.existsByVedtakId(vedtak.id)) {
            log.warn("Vedtak ${vedtak.id} er allerede arkivert")
            return 0
        }

        val hentPdf = hentPdf(fnr = vedtak.fnr, utbetalingId = vedtak.id)

        if (environmentToggles.isProduction()) {
            log.info("Arkiverer ikke vedtak ${vedtak.id} fordi vi ikke har skrudd dette på i produksjon ennå")
            return 0
        }

        val request = skapJournalpostRequest(vedtak, hentPdf.first)
        val journalpostResponse = dokArkivClient.opprettJournalpost(request, vedtak.id)

        if (!journalpostResponse.journalpostferdigstilt) {
            log.warn("Journalpost ${journalpostResponse.journalpostId} for vedtak ${vedtak.id} ble ikke ferdigstilt")
        }

        arkivertVedtakRepository.save(
            ArkivertVedtak(
                id = null,
                fnr = vedtak.fnr,
                vedtakId = vedtak.id,
                journalpostId = journalpostResponse.journalpostId,
                opprettet = Instant.now(),
                spinnsynArkiveringImage = naisAppImage,
                spinnsynFrontendImage = hentPdf.second
            )
        )
        log.info("Arkiverte vedtak ${vedtak.id}")
        registry.counter("vedtak_arkivert").increment()
        return 1
    }
}
