package no.nav.helse.flex.api

import no.nav.helse.flex.vedtak.Arkivaren
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@Unprotected
@Profile("testcontroller")
class TestController(
    val arkivaren: Arkivaren
) {

    @ResponseBody
    @GetMapping(value = ["/api/test/html/{fnr}/{utbetalingId}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHtml(@PathVariable fnr: String, @PathVariable utbetalingId: String): String {

        return arkivaren.hentSomHtmlOgInlineTing(fnr = fnr, utbetalingId = utbetalingId)
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/pdf/{fnr}/{utbetalingId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(@PathVariable fnr: String, @PathVariable utbetalingId: String): ByteArray {
        return arkivaren.hentPdf(fnr = fnr, utbetalingId = utbetalingId)
    }
}
