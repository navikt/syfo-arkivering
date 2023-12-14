package no.nav.helse.flex.pdfgenerering

import com.fasterxml.jackson.module.kotlin.readValue
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.slf4j.Slf4jLogger
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import com.openhtmltopdf.util.XRLog
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.pdfbox.io.IOUtils
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.TestAssertion
import java.io.*

object PdfGenerering {
    val colorProfile: ByteArray = IOUtils.toByteArray(this::class.java.getResourceAsStream("/sRGB2014.icc"))
    val fonts: List<FontMetadata> = objectMapper.readValue(this::class.java.getResourceAsStream("/fonts/config.json"))

    val log = logger()

    init {
        XRLog.setLoggerImpl(Slf4jLogger())
    }

    fun createPDFA(html: String): ByteArray {
        val pdf =
            ByteArrayOutputStream().apply {
                PdfRendererBuilder()
                    .apply {
                        for (font in fonts) {
                            useFont({ ByteArrayInputStream(font.bytes) }, font.family, font.weight, font.style, font.subset)
                        }
                    }
                    .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
                    .useColorProfile(colorProfile)
                    .useSVGDrawer(BatikSVGDrawer())
                    .withHtmlContent(html, null)
                    .toStream(this)
                    .run()
            }.toByteArray()
        require(verifyCompliance(pdf)) { "Non-compliant PDF/A :(" }
        return pdf
    }

    private fun verifyCompliance(
        input: ByteArray,
        flavour: PDFAFlavour = PDFAFlavour.PDFA_2_U,
    ): Boolean {
        val pdf = ByteArrayInputStream(input)
        val validator = Foundries.defaultInstance().createValidator(flavour, false)
        val result = Foundries.defaultInstance().createParser(pdf).use { validator.validate(it) }
        val failures =
            result.testAssertions
                .filter { it.status != TestAssertion.Status.PASSED }
        failures.forEach { test ->
            log.warn(test.message)
            log.warn("Location ${test.location.context} ${test.location.level}")
            log.warn("Status ${test.status}")
            log.warn("Test number ${test.ruleId.testNumber}")
        }
        return failures.isEmpty()
    }
}
