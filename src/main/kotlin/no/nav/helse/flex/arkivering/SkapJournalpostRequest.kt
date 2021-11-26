package no.nav.helse.flex.arkivering

import no.nav.helse.flex.client.domain.AvsenderMottaker
import no.nav.helse.flex.client.domain.Bruker
import no.nav.helse.flex.client.domain.Dokument
import no.nav.helse.flex.client.domain.Dokumentvarianter
import no.nav.helse.flex.client.domain.JournalpostRequest
import no.nav.helse.flex.client.domain.Sak
import no.nav.helse.flex.kafka.VedtakStatus

fun skapJournalpostRequest(
    vedtakStatus: VedtakStatus,
    pdf: ByteArray,
    tittel: String
): JournalpostRequest {
    return JournalpostRequest(
        bruker = Bruker(
            id = vedtakStatus.fnr,
            idType = "FNR"
        ),
        dokumenter = listOf(
            Dokument(
                dokumentvarianter = listOf(
                    Dokumentvarianter(
                        filnavn = tittel,
                        filtype = "PDFA",
                        variantformat = "ARKIV",
                        fysiskDokument = pdf
                    )
                ),
                tittel = tittel,
            )
        ),
        sak = Sak(
            sakstype = "GENERELL_SAK"
        ),
        journalpostType = "UTGAAENDE",
        journalfoerendeEnhet = "9999",
        eksternReferanseId = vedtakStatus.id,
        tema = "SYK",
        tittel = tittel,
        avsenderMottaker = AvsenderMottaker(
            id = vedtakStatus.fnr,
            idType = "FNR",
        )
    )
}
