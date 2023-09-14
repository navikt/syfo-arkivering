package no.nav.helse.flex.client

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.client.domain.JournalpostRequest
import no.nav.helse.flex.client.domain.JournalpostResponse
import no.nav.helse.flex.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Controller
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Controller
class DokArkivClient(
    private val dokarkivRestTemplate: RestTemplate,
    @Value("\${dokarkiv.url}") private val dokarkivUrl: String
) {

    @Retryable(backoff = Backoff(delay = 5000))
    fun opprettJournalpost(pdfRequest: JournalpostRequest, vedtakId: String): JournalpostResponse {
        try {
            val url = "$dokarkivUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"

            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers["Nav-Callid"] = vedtakId

            val entity = HttpEntity(pdfRequest, headers)

            // Kaster RestTemplateException for alle 4xx og 5xx HTTP statuskoder.
            val result = dokarkivRestTemplate.exchange(url, HttpMethod.POST, entity, JournalpostResponse::class.java)

            // TODO: Kommer ikke hit for annet enn 2xx og 3xx statuskoder så sjekken har ikke så mye verdi.
            if (!result.statusCode.is2xxSuccessful) {
                throw RuntimeException("dokarkiv feiler med HTTP-${result.statusCode} for vedtak med id: $vedtakId")
            }

            return result.body
                ?: throw RuntimeException("dokarkiv returnerer ikke data for vedtak med id: $vedtakId")
        } catch (e: HttpClientErrorException.Conflict) {
            return objectMapper.readValue(e.responseBodyAsString)
        }
    }
}
