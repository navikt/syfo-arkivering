package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arkivering.Arkivaren
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ArkiveringListener(
    val arkivaren: Arkivaren
) {

    @KafkaListener(
        topics = [FLEX_VEDTAK_ARKIVERING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val uarkivertVedtak = cr.value().tilUarkivertVedtak()
        arkivaren.arkiverUarkivertVedtak(uarkivertVedtak)
        acknowledgment.acknowledge()
    }

    fun String.tilUarkivertVedtak(): VedtakArkiveringDTO = objectMapper.readValue(this)
}

const val FLEX_VEDTAK_ARKIVERING_TOPIC = "flex.vedtak-arkivering"

data class VedtakArkiveringDTO(
    val fnr: String,
    val id: String,
)
