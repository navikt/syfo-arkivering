package no.nav.helse.flex

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
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

    companion object {
        var spinnsynArkiveringFrontendMockWebServer: MockWebServer

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
        }
    }

    @AfterAll
    fun `Vi tømmer databasen`() {
    }
}
