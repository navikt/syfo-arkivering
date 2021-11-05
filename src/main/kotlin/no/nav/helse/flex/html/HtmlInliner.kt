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
        #__next {
            margin: 0cm 0.7cm;
            height: 850px;
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
        val arkHeader = doc.selectFirst("#ark-header") ?: throw RuntimeException("Må ha ark header")
        val arkFooter = doc.selectFirst("#ark-footer") ?: throw RuntimeException("Må ha ark footer")
        val body = doc.selectFirst("body") ?: throw RuntimeException("Må ha html body")
        if (body.children().size != 1) {
            throw RuntimeException("Forventa bare en child til body")
        }
        val first = body.children().first()!!
        if (!first.hasAttr("id") || first.attr("id") != "__next") {
            throw RuntimeException("Forventa at første child har id __next")
        }
        arkHeader.remove()
        arkFooter.remove()
        body.child(0).before(arkHeader)
        body.appendChild(arkFooter)

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)

        return doc.toString()
    }
}
