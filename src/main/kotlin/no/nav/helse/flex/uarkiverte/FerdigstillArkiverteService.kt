package no.nav.helse.flex.uarkiverte

import no.nav.helse.flex.arkivering.ArkivertVedtakRepository
import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.client.FerdigstillJournalpostRequest
import no.nav.helse.flex.kafka.ArkivertVedtakDto
import no.nav.helse.flex.logger
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class FerdigstillArkiverteService(
    private val dokArkivClient: DokArkivClient,
    private val arkivertRepository: ArkivertVedtakRepository,
    private val spinnsynClient: SpinnsynBackendRestClient,
) {

    private val log = logger()

    fun ferdigstillVedtak(vedtakDto: ArkivertVedtakDto) {
        val journalPostId = hentJournalPostId(vedtakDto.id)

        if (journalPostId != null) {
            ferdigstillArkivertVedtak(journalPostId, vedtakDto)
        } else {
            log.info("Fant ikke vedtak med id: ${vedtakDto.id} i databasen med arkiverte vedtak. Avbryter ferdigstilling.")
        }
    }

    private fun ferdigstillArkivertVedtak(journalpostId: String, vedtakDto: ArkivertVedtakDto) {
        val datoJournal = hentOpprettetDato(vedtakDto.fnr, vedtakDto.id)

        val journalpostRequest = FerdigstillJournalpostRequest(
            journalfoerendeEnhet = "9999",
            datoJournal = datoJournal
        )
        dokArkivClient.ferdigstillJournalpost(
            journalpostId = journalpostId,
            journalpostRequest = journalpostRequest,
            vedtakId = vedtakDto.id
        )

        log.info("Ferdigstilt vedtak med id: ${vedtakDto.id}, journalpostId: $journalpostId og datoJournal: $datoJournal.")
    }

    private fun hentJournalPostId(id: String): String? {
        return try {
            arkivertRepository.getByVedtakId(id).journalpostId
        } catch (e: DataAccessException) {
            null
        }
    }

    private fun hentOpprettetDato(fnr: String, id: String): Instant {
        return spinnsynClient.hentVedtak(fnr).first { vedtak -> vedtak.id == id }.opprettetTimestamp
    }
}
