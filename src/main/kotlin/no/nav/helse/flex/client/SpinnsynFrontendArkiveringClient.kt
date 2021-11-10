package no.nav.helse.flex.client

import no.nav.helse.flex.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.OK
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class SpinnsynFrontendArkiveringClient(
    private val spinnsynFrontendArkiveringRestTemplate: RestTemplate,
    @Value("\${spinnsyn.frontend.arkivering.url}") private val url: String
) {

    val log = logger()

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
            log.error(message)
            throw RuntimeException(message)
        }

        val versjon = result.headers["x-nais-app-image"]?.first() ?: "UKJENT"
        result.body?.let { return HtmlVedtak(html = it, versjon = versjon) }

        val message = "Kall mot spinnsyn-frontend-arkivering returnerer ikke data"
        log.error(message)
        throw RuntimeException(message)
    }

    class HtmlVedtak(
        val html: String,
        val versjon: String,
    )
}
