package no.nav.helse.flex.uarkiverte

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Instant

@Component
class SpinnsynBackendRestClient(
    private val spinnsynBackendRestTemplate: RestTemplate,
    @Value("\${spinnsyn.backend.url}") private val spinnsynBackendUrl: String
) {

    @Retryable(exclude = [TomVedtaksListeException::class])
    fun hentVedtak(fnr: String): List<Vedtak> {
        val url = "$spinnsynBackendUrl/api/v1/arkivering/vedtak"

        val headers = HttpHeaders()
        headers["fnr"] = fnr
        val entity = HttpEntity<Any>(headers)

        // Kaster RestTemplateException for alle 4xx og 5xx HTTP statuskoder, som trigger retry.
        val result = spinnsynBackendRestTemplate.exchange(
            url, HttpMethod.GET, entity, Array<RSVedtakWrapper>::class.java
        )

        // Vi har en 200 OK, men med tom liste. Gjør ikke retry. Dette skal ikke skje da vi vet vi leser fnr fra
        // vedtak som allerede er arkivert.
        return result.body?.map { vedtakWrapper -> vedtakWrapper.tilVedtak() }?.toList()
            ?: throw TomVedtaksListeException("Spinnsyn Backend returnerte ingen vedtak.")
    }

    data class RSVedtakWrapper(
        val id: String,
        val opprettetTimestamp: Instant,
    )

    private fun RSVedtakWrapper.tilVedtak(): Vedtak = Vedtak(this.id, this.opprettetTimestamp)
}

data class Vedtak(
    val id: String,
    val opprettetTimestamp: Instant,
)

class TomVedtaksListeException(message: String) : RuntimeException(
    message
)
