package no.nav.helse.flex

import no.nav.helse.flex.arkivering.ArkivertVedtakRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider

private class PostgreSQLContainer12 : PostgreSQLContainer<PostgreSQLContainer12>("postgres:12-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@EnableMockOAuth2Server
abstract class Testoppsett {

    @Autowired
    lateinit var arkivertVedtakRepository: ArkivertVedtakRepository

    companion object {
        var spinnsynArkiveringFrontendMockWebServer: MockWebServer
        var dokarkivMockWebServer: MockWebServer

        init {
            VeraGreenfieldFoundryProvider.initialise()

            PostgreSQLContainer12().also {
                it.start()
                System.setProperty("spring.datasource.url", it.jdbcUrl)
                System.setProperty("spring.datasource.username", it.username)
                System.setProperty("spring.datasource.password", it.password)
            }

            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.1.1")).also {
                it.start()
                System.setProperty("KAFKA_BROKERS", it.bootstrapServers)
            }

            spinnsynArkiveringFrontendMockWebServer = MockWebServer()
                .also { it.start() }
                .also {
                    System.setProperty("spinnsyn.frontend.arkivering.url", "http://localhost:${it.port}")
                }

            dokarkivMockWebServer = MockWebServer()
                .also { it.start() }
                .also {
                    System.setProperty("dokarkiv.url", "http://localhost:${it.port}")
                }
        }
    }

    @AfterAll
    fun `Vi tømmer databasen`() {
        arkivertVedtakRepository.deleteAll()
    }

    fun enqueFiler() {
        enqueFil("/testside.html", imageName = "docker.github/spinnsyn-frontend-v2.0", fom = "2020-03-12", tom = "2020-04-30")
        enqueFil("/stylesheet.css")
        enqueFil("/ikon-skriv-til-oss.svg")
    }

    fun enqueFil(fil: String, imageName: String? = null, fom: String? = null, tom: String? = null) {
        val innhold = HentingOgPdfGenereringTest::class.java.getResource(fil).readText()

        val response = MockResponse().setBody(innhold)
        imageName?.let {
            response.setHeader("x-nais-app-image", it)
        }
        tom?.let {
            response.setHeader("x-vedtak-tom", it)
        }
        fom?.let {
            response.setHeader("x-vedtak-fom", it)
        }

        spinnsynArkiveringFrontendMockWebServer.enqueue(response)
    }

    fun validerRequests(uuid: String, fnr: String) {
        val htmlRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        htmlRequest.path `should be equal to` "/syk/sykepenger/vedtak/arkivering/$uuid"
        htmlRequest.headers["fnr"] `should be equal to` fnr
        htmlRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")

        val stylesheetRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        stylesheetRequest.path `should be equal to` "/syk/sykepenger/_next/static/css/yes.css"
        stylesheetRequest.headers["fnr"].shouldBeNull()
        stylesheetRequest.headers["Authorization"].shouldBeNull()

        val svgRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        svgRequest.path `should be equal to` "/public/ikon-skriv-til-oss.svg"
        svgRequest.headers["fnr"].shouldBeNull()
        svgRequest.headers["Authorization"].shouldBeNull()
    }
}
