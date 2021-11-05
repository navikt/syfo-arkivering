package no.nav.helse.flex.html

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*

@Component
class HtmlInliner(
    @Value("\${spinnsyn.frontend.arkivering.url}") private val url: String,
) {

    fun inlineHtml(html: String): String {

        val doc = Jsoup.parse(html)
        doc.select("link").forEach {
            if (it.hasAttr("href")) {
                val href = it.attr("href")
                if (!href.endsWith(".css")) {
                    throw RuntimeException("Link med href som ikke er .css")
                }
                val stylesheet = URL(url + href).readText()
                it.parent()?.append("<style>\n$stylesheet\n</style>")
                it.remove()
            } else {
                throw RuntimeException("Link uten href")
            }
        }
        val tvungenFontStyle = """
    <style>
        * {
            font-family: "Source Sans Pro" !important;
        }
    </style>            
        """
        doc.select("head").forEach {
            it.append(tvungenFontStyle)
        }
        doc.select("script").forEach {
            it.remove()
        }
        doc.select("meta").forEach {
            it.remove()
        }
        doc.select("svg").forEach {
            if (!it.hasAttr("xmlns")) {
                it.attr("xmlns", "http://www.w3.org/2000/svg")
            }
        }
        doc.select("img").forEach {
            if (it.hasAttr("src")) {
                val bildeUrl = url + it.attr("src")
                if (!bildeUrl.endsWith(".svg")) {
                    throw RuntimeException("Støtter kun svg. Kan ikke laste ned bilde $bildeUrl")
                }
                val img = URL(bildeUrl).readText()
                if (!img.contains("http://www.w3.org/2000/svg")) {
                    throw RuntimeException("$bildeUrl mangler xmlns:http://www.w3.org/2000/svg tag")
                }
                val b64img = Base64.getEncoder().encodeToString(img.toByteArray())
                it.removeAttr("src")
                it.attr("src", "data:image/svg+xml;base64,$b64img")
            }
        }
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)

        return doc.toString()
    }
}
