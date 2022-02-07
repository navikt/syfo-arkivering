package no.nav.helse.flex.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.OK
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate

@Component
class SpinnsynFrontendArkiveringClient(
    private val spinnsynFrontendArkiveringRestTemplate: RestTemplate,
    @Value("\${spinnsyn.frontend.arkivering.url}") private val url: String
) {

    @Retryable(exclude = [VedtakIkkeFunnetException::class])
    fun hentVedtakSomHtml(fnr: String, id: String): HtmlVedtak {
        try {
            return hentVedtak(fnr = fnr, id = id)
        } catch (e: HttpClientErrorException.NotFound) {
            // Forhindrer retry når vi vet at vedtaket ikke finnes.
            throw VedtakIkkeFunnetException("Vedtak med id: $id ble ikke returnert fra spinnsyn-frontend (404 Not Found)")
        }
    }

    private fun hentVedtak(fnr: String, id: String): HtmlVedtak {
        val uriBuilder = UriComponentsBuilder.fromHttpUrl("$url/syk/sykepenger/vedtak/arkivering/$id")

        val headers = HttpHeaders()
        headers["fnr"] = fnr

        // Kaster RestTemplateException for alle 4xx og 5xx HTTP statuskoder.
        val result = spinnsynFrontendArkiveringRestTemplate
            .exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                String::class.java
            )

        // TODO: Kommer ikke hit for annet enn 2xx og 3xx statuskoder så sjekken har ikke så mye verdi.
        if (result.statusCode != OK) {
            throw RuntimeException("Kall mot spinnsyn-frontend-arkivering feiler med HTTP-${result.statusCode}")
        }

        val versjon =
            result.headers["x-nais-app-image"]?.first() ?: throw RuntimeException("Påkrevd header x-nais-app-image")
        val fom = result.headers["x-vedtak-fom"]?.first() ?: throw RuntimeException("Påkrevd header x-vedtak-fom")
        val tom = result.headers["x-vedtak-tom"]?.first() ?: throw RuntimeException("Påkrevd header x-vedtak-tom")

        result.body?.let {
            return HtmlVedtak(
                html = it,
                versjon = versjon,
                fom = LocalDate.parse(fom),
                tom = LocalDate.parse(tom)
            )
        }

        throw RuntimeException("Kall mot spinnsyn-frontend-arkivering returnerer ikke data")
    }

    data class HtmlVedtak(
        val html: String,
        val versjon: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )
}

class VedtakIkkeFunnetException(message: String) : RuntimeException(
    message
)
