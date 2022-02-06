package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.uarkiverte.FerdigstillArkiverteService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ArkiveringListener(
    private val ferdigstillArkiverteService: FerdigstillArkiverteService
) {

    private val log = logger()

    @KafkaListener(
        topics = [FLEX_VEDTAK_ARKIVERING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
        groupId = "spinnsyn-arkivering-ferdigstilling"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val arkivertVedtakDto = cr.value().tilArkivertVedtakDto()
        log.info(
            "Lest vedtak med id: ${arkivertVedtakDto.id} fra topic: $FLEX_VEDTAK_ARKIVERING_TOPIC, " +
                "partisjon: ${cr.partition()} og offset: ${cr.offset()}."
        )
        ferdigstillArkiverteService.ferdigstillVedtak(arkivertVedtakDto)
        acknowledgment.acknowledge()
    }

    fun String.tilArkivertVedtakDto(): ArkivertVedtakDto = objectMapper.readValue(this)
}

const val FLEX_VEDTAK_ARKIVERING_TOPIC = "flex.vedtak-arkivering"

data class ArkivertVedtakDto(
    val fnr: String,
    val id: String,
)
