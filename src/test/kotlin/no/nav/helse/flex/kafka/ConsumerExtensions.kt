package no.nav.helse.flex.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Duration

fun <K, V> Consumer<K, V>.lyttPaaTopic(vararg topics: String) {
    if (this.subscription().isEmpty()) {
        this.subscribe(listOf(*topics))
    }
}

fun <K, V> Consumer<K, V>.hentRecords(duration: Duration = Duration.ofMillis(100)): List<ConsumerRecord<K, V>> {
    return this.poll(duration).also {
        this.commitSync()
    }.iterator().asSequence().toList()
}
