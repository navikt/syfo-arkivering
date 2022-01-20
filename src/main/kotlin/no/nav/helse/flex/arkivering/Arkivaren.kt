package no.nav.helse.flex.arkivering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.html.HtmlInliner
import no.nav.helse.flex.kafka.VedtakStatus
import no.nav.helse.flex.kafka.VedtakStatusDto
import no.nav.helse.flex.logger
import no.nav.helse.flex.pdfgenerering.PdfGenerering.createPDFA
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class Arkivaren(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient,
    val htmlInliner: HtmlInliner,
    val dokArkivClient: DokArkivClient,
    val arkivertVedtakRepository: ArkivertVedtakRepository,
    val registry: MeterRegistry,
    @Value("\${nais.app.image}")
    val naisAppImage: String
) {

    val log = logger()

    val norskDato: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    data class PdfVedtak(
        val pdf: ByteArray,
        val versjon: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    fun arkiverVedtak(vedtak: VedtakStatusDto): Int {
        if (vedtak.vedtakStatus != VedtakStatus.MOTATT) {
            return 0
        }
        if (arkivertVedtakRepository.existsByVedtakId(vedtak.id)) {
            log.warn("Vedtak med $vedtak.id er allerede arkivert")
            return 0
        }
        return lagreJournalpost(fnr = vedtak.fnr, id = vedtak.id)
    }

    fun hentPdf(fnr: String, id: String): PdfVedtak {
        val html = hentSomHtmlOgInlineTing(fnr = fnr, id = id)

        val pdf = hentPdfFraHtml(html.html)
        return PdfVedtak(
            pdf = pdf,
            versjon = html.versjon,
            fom = html.fom,
            tom = html.tom
        )
    }

    fun hentSomHtmlOgInlineTing(fnr: String, id: String): SpinnsynFrontendArkiveringClient.HtmlVedtak {
        val htmlVedtak = spinnsynFrontendArkiveringClient.hentVedtakSomHtml(fnr = fnr, id = id)
        return htmlVedtak.copy(html = htmlInliner.inlineHtml(htmlVedtak.html))
    }

    fun hentPdfFraHtml(html: String): ByteArray {
        val nyDoctype = """
            <!DOCTYPE html PUBLIC
 "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN" "">
        """.trimIndent()

        return createPDFA(html.replaceFirst("<!DOCTYPE html>", nyDoctype))
    }

    private fun lagreJournalpost(fnr: String, id: String): Int {
        val vedtaket = hentPdf(fnr = fnr, id = id)

        val tittel = "Svar på søknad om sykepenger for periode: ${vedtaket.fom.format(norskDato)} " +
            "til ${vedtaket.tom.format(norskDato)}"
        val request = skapJournalpostRequest(fnr, id, vedtaket.pdf, tittel)
        val journalpostResponse = dokArkivClient.opprettJournalpost(request, id)

        if (!journalpostResponse.journalpostferdigstilt) {
            log.warn("Journalpost ${journalpostResponse.journalpostId} for vedtak $id ble ikke ferdigstilt")
        }

        arkivertVedtakRepository.save(
            ArkivertVedtak(
                id = null,
                fnr = fnr,
                vedtakId = id,
                journalpostId = journalpostResponse.journalpostId,
                opprettet = Instant.now(),
                spinnsynArkiveringImage = naisAppImage,
                spinnsynFrontendImage = vedtaket.versjon
            )
        )
        log.info("Arkiverte vedtak $id")
        registry.counter("vedtak_arkivert").increment()
        return 1
    }
}
