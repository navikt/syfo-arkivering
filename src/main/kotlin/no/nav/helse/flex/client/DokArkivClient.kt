package no.nav.helse.flex.client

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.helse.flex.client.domain.JournalpostRequest
import no.nav.helse.flex.client.domain.JournalpostResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate
import java.time.Instant

@Controller
class DokArkivClient(
    private val dokarkivRestTemplate: RestTemplate,
    @Value("\${dokarkiv.url}") private val dokarkivUrl: String
) {

    @Retryable(backoff = Backoff(delay = 5000))
    fun opprettJournalpost(pdfRequest: JournalpostRequest, vedtakId: String): JournalpostResponse {
        val url = "$dokarkivUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Nav-Callid"] = vedtakId

        val entity = HttpEntity(pdfRequest, headers)

        val result = dokarkivRestTemplate.exchange(url, HttpMethod.POST, entity, JournalpostResponse::class.java)

        if (!result.statusCode.is2xxSuccessful) {
            throw RuntimeException("dokarkiv feiler med HTTP-${result.statusCode} for vedtak med id: $vedtakId")
        }

        return result.body
            ?: throw RuntimeException("dokarkiv returnerer ikke data for vedtak med id: $vedtakId")
    }

    fun ferdigstillJournalpost(
        journalpostId: String,
        journalpostRequest: FerdigstillJournalpostRequest,
        vedtakId: String
    ) {
        val url = "$dokarkivUrl/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(journalpostRequest, headers)
        dokarkivRestTemplate.exchange(url, HttpMethod.PATCH, httpEntity, Void::class.java)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FerdigstillJournalpostRequest(
    val journalfoerendeEnhet: String,
    val datoJournal: Instant
)
