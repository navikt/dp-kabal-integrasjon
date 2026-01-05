package no.nav.dagpenger.klageinstans

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.River.PacketListener
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class KlageBehovløser(
    rapidsConnection: RapidsConnection,
    private val klageKlient: KlageHttpKlient,
) : PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAll("@behov", listOf("OversendelseKlageinstans"))
                it.forbid("@løsning")
            }
            validate {
                it.requireKey(
                    "ident",
                    "behandlingId",
                    "fagsakId",
                    "behandlendeEnhet",
                    "hjemler",
                    "opprettet",
                )
            }
            validate {
                it.interestedIn(
                    "kommentar",
                    "tilknyttedeJournalposter",
                    "prosessfullmektigNavn",
                    "prosessfullmektigIdent",
                    "prosessfullmektigAdresselinje1",
                    "prosessfullmektigAdresselinje2",
                    "prosessfullmektigAdresselinje3",
                    "prosessfullmektigPostnummer",
                    "prosessfullmektigPoststed",
                    "prosessfullmektigLand",
                )
            }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        if (behandlingId in setOf("019b2712-1bff-7474-b005-5be09fc47d7a")) {
            logger.info { "Skipper oversendelse av klagebehandling $behandlingId" }
            return
        }
        withLoggingContext("behandlingId" to "$behandlingId") {
            logger.info { "Mottatt behov om oversendelse av klage til klageinstans for behandling $behandlingId" }
            sikkerlogg.info { "Behandlingsdata for klagebehandling $behandlingId: ${packet.toJson()}" }

            val ident = packet["ident"].asText()
            val fagsakId = packet["fagsakId"].asText()
            val opprettet = packet["opprettet"].asLocalDateTime()
            val behandlendeEnhet = packet["behandlendeEnhet"].asText()
            val hjemler = packet["hjemler"].map { it.asText() }
            val tilknyttedeJournalposter: List<Journalposter> =
                packet["tilknyttedeJournalposter"].takeIf(JsonNode::isArray)?.map {
                    it.takeIf(JsonNode::isObject).let { jp ->
                        Journalposter(
                            jp?.get("type")!!.asText(),
                            jp.get("journalpostId")!!.asText(),
                        )
                    }
                } ?: emptyList()
            val kommentar = packet["kommentar"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigNavn = packet["prosessfullmektigNavn"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigIdent = packet["prosessfullmektigIdent"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigAdresselinje1 =
                packet["prosessfullmektigAdresselinje1"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigAdresselinje2 =
                packet["prosessfullmektigAdresselinje2"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigAdresselinje3 =
                packet["prosessfullmektigAdresselinje3"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigPostnummer =
                packet["prosessfullmektigPostnummer"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigPoststed = packet["prosessfullmektigPoststed"].takeIf(JsonNode::isTextual)?.asText()
            val prosessfullmektigLand = packet["prosessfullmektigLand"].takeIf(JsonNode::isTextual)?.asText()

            val prosessFullmektig =
                if (!prosessfullmektigNavn.isNullOrBlank() || !prosessfullmektigIdent.isNullOrBlank()) {
                    ProsessFullmektig(
                        id = if (!prosessfullmektigIdent.isNullOrBlank()) PersonIdentId(verdi = prosessfullmektigIdent) else null,
                        navn = prosessfullmektigNavn,
                        adresse =
                            if (!prosessfullmektigLand.isNullOrBlank()) {
                                Adresse(
                                    addresselinje1 = prosessfullmektigAdresselinje1,
                                    addresselinje2 = prosessfullmektigAdresselinje2,
                                    addresselinje3 = prosessfullmektigAdresselinje3,
                                    postnummer = prosessfullmektigPostnummer,
                                    poststed = prosessfullmektigPoststed,
                                    land = prosessfullmektigLand,
                                )
                            } else {
                                null
                            },
                    )
                } else {
                    null
                }

            runBlocking {
                klageKlient.oversendKlageAnke(
                    behandlingId = behandlingId,
                    ident = ident,
                    fagsakId = fagsakId,
                    behandlendeEnhet = behandlendeEnhet,
                    hjemler = hjemler,
                    tilknyttedeJournalposter = tilknyttedeJournalposter,
                    prosessFullmektig = prosessFullmektig,
                    opprettet = opprettet.toLocalDate(),
                    kommentar = kommentar,
                )
            }.also { resultat ->
                when (resultat.isSuccess) {
                    true -> {
                        logger.info { "Klage er oversendt til klageinstans for behandling $behandlingId" }
                        packet["@løsning"] = mapOf("OversendelseKlageinstans" to "OK")
                        context.publish(key = ident, message = packet.toJson())
                    }

                    false -> {
                        logger.info { "Feil ved oversendelse til klageinstans for behandling $behandlingId" }
                        throw RuntimeException("Feil ved oversendelse av klage til klageinstans for behandling $behandlingId")
                    }
                }
            }
        }
    }
}
