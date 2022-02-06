package no.nav.helse.flex

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.client.FerdigstillJournalpostRequest
import no.nav.helse.flex.kafka.ArkivertVedtakDto
import no.nav.helse.flex.kafka.FLEX_VEDTAK_ARKIVERING_TOPIC
import no.nav.helse.flex.kafka.hentRecords
import no.nav.helse.flex.kafka.lyttPaaTopic
import no.nav.helse.flex.uarkiverte.SpinnsynBackendRestClient.RSVedtakWrapper
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldStartWith
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.nio.charset.Charset
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class FerdigstillArkiverteIntegrasjonsTest() : Testoppsett() {

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    @Autowired
    private lateinit var uarkiverteKafkaConsumer: Consumer<String, String>

    @BeforeAll
    fun subscribeTilTopic() {
        uarkiverteKafkaConsumer.lyttPaaTopic(FLEX_VEDTAK_ARKIVERING_TOPIC)
        uarkiverteKafkaConsumer.hentRecords().shouldBeEmpty()
    }

    fun `Arkivert vedtak blir ferdigstilt`() {
        val vedtakId = "88804146-28ee-30d9-b0a3-a904919c7a37"
        val fnr = "fnr-1"
        val journalpostId = "journalpost-1"
        val opprettet = LocalDateTime.of(2022, 5, 1, 12, 5, 10).toInstant(ZoneOffset.UTC)

        opprettArkivertVedtak(vedtakId, fnr, journalpostId)

        val spinnsynBackendResponse = listOf(RSVedtakWrapper(id = vedtakId, opprettetTimestamp = opprettet))
        val mockResponse = MockResponse().setBody(spinnsynBackendResponse.serialisertTilString())
        spinnsynBackendMockWebServer.enqueue(mockResponse)

        val dokarkivResponse = MockResponse().setResponseCode(200)
        dokarkivMockWebServer.enqueue(dokarkivResponse)

        kafkaProducer.send(
            ProducerRecord(
                FLEX_VEDTAK_ARKIVERING_TOPIC,
                null,
                fnr,
                ArkivertVedtakDto(fnr = fnr, id = vedtakId).serialisertTilString()
            )
        ).get()

        val htmlRequest = spinnsynBackendMockWebServer.takeRequest()

        htmlRequest.path `should be equal to` "/api/v1/arkivering/vedtak"
        htmlRequest.headers["fnr"] `should be equal to` fnr
        htmlRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")

        val dokarkivRequest = dokarkivMockWebServer.takeRequest()
        dokarkivRequest.path `should be equal to` "/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill"
        dokarkivRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")

        val journalpostRequestBody: FerdigstillJournalpostRequest =
            objectMapper.readValue(dokarkivRequest.body.readString(Charset.defaultCharset()))

        journalpostRequestBody.datoJournal `should be equal to` opprettet
        journalpostRequestBody.journalfoerendeEnhet `should be equal to` "9999"

        dokarkivMockWebServer.requestCount `should be equal to` 1
    }

    private fun opprettArkivertVedtak(vedtakId: String, fnr: String, journalpostId: String) {
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO arkivert_vedtak (id, vedtak_id, fnr, journalpost_id, opprettet, spinnsyn_frontend_image, spinnsyn_arkivering_image) 
            VALUES (:id, :vedtak_id, :fnr, :journalpost_id, :opprettet, :frontend_image, :arkivering_image)
        """,
            MapSqlParameterSource()
                .addValue("id", UUID.randomUUID().toString())
                .addValue("vedtak_id", vedtakId)
                .addValue("fnr", fnr)
                .addValue("journalpost_id", journalpostId)
                .addValue("opprettet", Timestamp.from(Instant.now()))
                .addValue("frontend_image", "frontend_image")
                .addValue("arkivering_image", "arkivering_image")
        )
    }
}
