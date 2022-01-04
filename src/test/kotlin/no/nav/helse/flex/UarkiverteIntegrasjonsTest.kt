package no.nav.helse.flex

import no.nav.helse.flex.client.domain.JournalpostResponse
import no.nav.helse.flex.kafka.FLEX_VEDTAK_ARKIVERING_TOPIC
import no.nav.helse.flex.kafka.VedtakArkiveringDTO
import no.nav.helse.flex.kafka.hentRecords
import no.nav.helse.flex.kafka.lyttPaaTopic
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEmpty
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.util.*
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UarkiverteIntegrasjonsTest : Testoppsett() {

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    @Autowired
    private lateinit var uarkiverteKafkaConsumer: Consumer<String, String>

    @BeforeAll
    fun subscribeTilTopic() {
        uarkiverteKafkaConsumer.lyttPaaTopic(FLEX_VEDTAK_ARKIVERING_TOPIC)
        uarkiverteKafkaConsumer.hentRecords().shouldBeEmpty()
    }

    private val vedtakId = UUID.randomUUID().toString()
    private val fnr = "12345678987"

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
                FLEX_VEDTAK_ARKIVERING_TOPIC,
                null,
                fnr,
                VedtakArkiveringDTO(fnr = fnr, id = vedtakId).serialisertTilString()
            )
        ).get()

        await().atMost(10, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.existsByVedtakId(vedtakId)
        }

        validerRequests(vedtakId, fnr)
        val journalfoeringRequest = dokarkivMockWebServer.takeRequest()
        journalfoeringRequest.path `should be equal to` "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"

        val arkivertVedtak = arkivertVedtakRepository.findAll().first { it.vedtakId == vedtakId }
        arkivertVedtak.vedtakId `should be equal to` vedtakId
    }

    @Test
    @Order(2)
    fun `mottar et duplikat vedtak som ikke arkiveres`() {

        arkivertVedtakRepository.count() `should be equal to` 1L

        kafkaProducer.send(
            ProducerRecord(
                FLEX_VEDTAK_ARKIVERING_TOPIC,
                null,
                fnr,
                VedtakArkiveringDTO(fnr = fnr, id = vedtakId).serialisertTilString()
            )
        ).get()

        await().during(5, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.count() == 1L
        }
    }

    @Test
    @Order(3)
    fun `arkiverer et vedtak som ikke finnes`() {

        val uuid = UUID.randomUUID().toString()

        val mockResponse = MockResponse().setBody("Test").setResponseCode(404)
        spinnsynArkiveringFrontendMockWebServer.enqueue(mockResponse)

        kafkaProducer.send(
            ProducerRecord(
                FLEX_VEDTAK_ARKIVERING_TOPIC,
                null,
                fnr,
                VedtakArkiveringDTO(fnr = fnr, id = uuid).serialisertTilString()
            )
        ).get()

        val htmlRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        htmlRequest.path `should be equal to` "/syk/sykepenger/vedtak/arkivering/$uuid"

        uarkiverteKafkaConsumer.hentRecords().shouldBeEmpty()
    }
}
