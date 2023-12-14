package no.nav.helse.flex.pdfgenerering

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder

data class FontMetadata(
    val family: String,
    val path: String,
    val weight: Int,
    val style: BaseRendererBuilder.FontStyle,
    val subset: Boolean,
) {
    val bytes: ByteArray = FontMetadata::class.java.getResourceAsStream("/fonts/" + path).readAllBytes()
}
