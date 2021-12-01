package no.nav.helse.flex.client

import no.nav.helse.flex.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.OK
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate

@Component
class SpinnsynFrontendArkiveringClient(
    private val spinnsynFrontendArkiveringRestTemplate: RestTemplate,
    @Value("\${spinnsyn.frontend.arkivering.url}") private val url: String
) {

    val log = logger()

    @Retryable
    fun hentVedtakSomHtml(utbetalingId: String, fnr: String): HtmlVedtak {

        val uriBuilder = UriComponentsBuilder.fromHttpUrl("$url/syk/sykepenger/vedtak/arkivering/$utbetalingId")

        val headers = HttpHeaders()
        headers["fnr"] = fnr

        val result = spinnsynFrontendArkiveringRestTemplate
            .exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                String::class.java
            )

        if (result.statusCode != OK) {
            val message = """Kall mot spinnsyn-frontend-arkivering feiler med HTTP-${result.statusCode}"""
            log.warn(message)
            throw RuntimeException(message)
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

        val message = "Kall mot spinnsyn-frontend-arkivering returnerer ikke data"
        log.error(message)
        throw RuntimeException(message)
    }

    data class HtmlVedtak(
        val html: String,
        val versjon: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
