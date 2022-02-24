package no.nav.helse.flex.arkivering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.kafka.VedtakStatus
import no.nav.helse.flex.kafka.VedtakStatusDto
import no.nav.helse.flex.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class Arkivaren(
    val pdfSkaperen: PdfSkaperen,
    val dokArkivClient: DokArkivClient,
    val arkivertVedtakRepository: ArkivertVedtakRepository,
    val registry: MeterRegistry,
    @Value("\${nais.app.image}")
    val naisAppImage: String
) {

    val log = logger()

    val norskDato: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

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

    private fun lagreJournalpost(fnr: String, id: String): Int {
        val vedtaket = pdfSkaperen.hentPdf(fnr = fnr, id = id)

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
