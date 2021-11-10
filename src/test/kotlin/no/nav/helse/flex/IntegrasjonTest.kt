package no.nav.helse.flex

import no.nav.helse.flex.client.domain.JournalpostResponse
import no.nav.helse.flex.kafka.FLEX_VEDTAK_STATUS_TOPIC
import no.nav.helse.flex.kafka.VedtakStatus
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldStartWith
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.util.*
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class IntegrasjonTest : Testoppsett() {

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    val vedtakId = UUID.randomUUID().toString()
    val fnr = "12345678987"

    @Test
    @Order(1)
    fun `mottar et vedtak som skal arkiveres`() {

        enqueFiler()
        val journalpostResponse = JournalpostResponse(
            dokumenter = emptyList(),
            journalpostId = "jpostid123",
            journalpostferdigstilt = true
        )
        val response = MockResponse()
            .setBody(journalpostResponse.serialisertTilString())
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        dokarkivMockWebServer.enqueue(response)

        kafkaProducer.send(
            ProducerRecord(
                FLEX_VEDTAK_STATUS_TOPIC,
                null,
                fnr,
                VedtakStatus(id = vedtakId, fnr = fnr).serialisertTilString()
            )
        ).get()

        await().atMost(10, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.existsByVedtakId(vedtakId)
        }

        validerRequests(vedtakId, fnr)
        val journalfoeringRequest = dokarkivMockWebServer.takeRequest()
        journalfoeringRequest.path `should be equal to` "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
        journalfoeringRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")
        journalfoeringRequest.headers["Nav-Callid"] `should be equal to` vedtakId

        val arkivertVedtak = arkivertVedtakRepository.findAll().first { it.vedtakId == vedtakId }

        arkivertVedtak.vedtakId `should be equal to` vedtakId
        arkivertVedtak.journalpostId `should be equal to` "jpostid123"
        arkivertVedtak.fnr `should be equal to` fnr
        arkivertVedtak.spinnsynArkiveringImage `should be equal to` "docker.github/spinnsyn-arkivering-v2.0"
        arkivertVedtak.spinnsynFrontendImage `should be equal to` "docker.github/spinnsyn-frontend-v2.0"
    }

    @Test
    @Order(2)
    fun `mottar et duplikat vedtak som ikke arkiveres`() {

        arkivertVedtakRepository.count() `should be equal to` 1L
        kafkaProducer.send(
            ProducerRecord(
                FLEX_VEDTAK_STATUS_TOPIC,
                null,
                fnr,
                VedtakStatus(id = vedtakId, fnr = fnr).serialisertTilString()
            )
        ).get()

        await().during(5, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.count() == 1L
        }
    }
}
