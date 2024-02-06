package no.nav.helse.flex.html

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

@Component
class HtmlInliner(
    @Value("\${spinnsyn.frontend.arkivering.url}") private val url: String,
) {
    var clock = Clock.systemDefaultZone()

    val stylingCss =
        this::class.java.getResourceAsStream("/arkivering/styling.css").readBytes().toString(Charsets.UTF_8)

    val footer =
        this::class.java.getResourceAsStream("/arkivering/footer.html").readBytes().toString(Charsets.UTF_8)
    val header =
        this::class.java.getResourceAsStream("/arkivering/header.html").readBytes().toString(Charsets.UTF_8)
    val personinfo =
        this::class.java.getResourceAsStream("/arkivering/personinfo.html").readBytes().toString(Charsets.UTF_8)

    val navSvg = this::class.java.getResourceAsStream("/arkivering/nav.svg").readBytes().toString(Charsets.UTF_8)
    val navSvgB64 =
        "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(navSvg.toByteArray(Charsets.UTF_8))

    fun inlineHtml(
        html: String,
        utbetalingId: String,
        fnr: String,
    ): String {
        val doc = Jsoup.parse(html)
        doc.select("link").forEach {
            val rel = it.attr("rel")
            if (rel != "stylesheet") {
                it.remove()
                return@forEach
            }
            if (it.hasAttr("href")) {
                val href = it.attr("href")
                if (!href.endsWith(".css")) {
                    throw RuntimeException("Link med href som ikke er .css")
                }
                val adresse =
                    if (href.startsWith("http")) {
                        href
                    } else {
                        "$url$href"
                    }

                @Suppress("DEPRECATION")
                val stylesheet =
                    URI(adresse).toURL().readText()
                        .replace("@media print", "@media papirprint")
                        .replace("&", "&amp;")

                it.parent()?.append("<style>\n$stylesheet\n</style>")
                it.remove()
            } else {
                throw RuntimeException("Link uten href")
            }
        }
        doc.select(".flex").forEach {
            if (!(it.hasClass("arkivering-flex-fix") || it.hasClass("flex-arkivering-ignore"))) {
                throw RuntimeException("Flex-styling er ikke støttet")
            }
            if (it.hasClass("arkivering-flex-fix")) {
                if (it.childrenSize() != 2) {
                    throw RuntimeException("arkivering-flex-fix må ha 2 children")
                }

                it.removeClass("flex")
                it.firstChild()!!.attr("style", "display: inline-block; width: 49%;")
                it.lastChild()!!.attr("style", "display: inline-block; width: 49%; text-align: right;")
            }
        }

        doc.select("head").forEach {
            it.append(
                """
    <style>
$stylingCss
    </style>            
        """,
            )
        }
        doc.select("script").forEach {
            it.remove()
        }
        doc.select("meta").forEach {
            it.remove()
        }
        doc.select("svg").forEach {
            it.remove()
        }
        doc.select("img").forEach {
            it.remove()
        }
        val arkHeader = Jsoup.parse(header.replace("##NAVLOGOSVG##", navSvgB64))
        val arkFooter = Jsoup.parse(footer.replace("##UTBETALINGID##", utbetalingId))
        val personinfo =
            Jsoup.parse(
                personinfo
                    .replace("##FNR##", fnrForVisning(fnr))
                    .replace("##TIDSSTEMPEL##", tidsstempel()),
            )
        val body = doc.selectFirst("body") ?: throw RuntimeException("Må ha html body")
        if (body.children().size != 1) {
            throw RuntimeException("Forventa bare en child til body")
        }
        val first = body.children().first()!!
        if (!first.hasAttr("id") || first.attr("id") != "__next") {
            throw RuntimeException("Forventa at første child har id __next")
        }

        body.child(0).child(0).removeAttr("class")
        body.child(0).child(0).before(personinfo.body().firstChild()!!)
        body.child(0).before(arkHeader.body().firstChild()!!)
        body.appendChild(arkFooter.body().firstChild()!!)

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)

        return doc.toString()
    }

    fun fnrForVisning(fnr: String): String {
        return "${fnr.slice(0..5)} ${fnr.slice(6 until fnr.length)}"
    }

    fun tidsstempel(): String {
        val currentDateTimeInOslo = Instant.now(clock).atZone(ZoneId.of("Europe/Oslo")).withNano(0)
        val formatter =
            DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffsetId()
                .optionalStart()
                .parseCaseSensitive()
                .toFormatter()
        return currentDateTimeInOslo.format(formatter)
    }
}
