package no.nav.helse.flex

import com.nhaarman.mockitokotlin2.whenever
import no.nav.helse.flex.arkivering.ArkivertVedtakRepository
import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.kafka.ArkivertVedtakDto
import no.nav.helse.flex.uarkiverte.FerdigstillArkiverteService
import no.nav.helse.flex.uarkiverte.SpinnsynBackendRestClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataRetrievalFailureException

@ExtendWith(MockitoExtension::class)
class FerdigstillArkiverteTest {

    @Mock
    private lateinit var spinnsynRestClient: SpinnsynBackendRestClient

    @Mock
    private lateinit var dokarkivRestClient: DokArkivClient

    @Mock
    private lateinit var arkiverteRepository: ArkivertVedtakRepository

    @InjectMocks
    private lateinit var ferdigstillArkiverteService: FerdigstillArkiverteService

    @Test
    fun `REST-endepunkt blir ikke kalt når vedtaket ikke er arkivert`() {

        whenever(arkiverteRepository.getByVedtakId("vedtak-1"))
            .thenThrow(DataRetrievalFailureException("Test"))

        ferdigstillArkiverteService.ferdigstillVedtak(ArkivertVedtakDto("fnr-1", "vedtak-1"))

        verifyNoInteractions(spinnsynRestClient)
        verifyNoInteractions(dokarkivRestClient)
    }
}
