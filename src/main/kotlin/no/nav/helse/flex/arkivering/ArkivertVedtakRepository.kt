package no.nav.helse.flex.arkivering

import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ArkivertVedtakRepository : CrudRepository<ArkivertVedtak, String> {
    fun existsByVedtakId(vedtakId: String): Boolean
    fun getByVedtakId(vedtakId: String): ArkivertVedtak
}

data class ArkivertVedtak(
    @Id
    val id: String? = null,
    val fnr: String,
    val vedtakId: String,
    val journalpostId: String,
    val opprettet: Instant,
    val spinnsynFrontendImage: String,
    val spinnsynArkiveringImage: String,
)
