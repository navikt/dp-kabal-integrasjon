package no.nav.dagpenger.klageinstans

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.River.PacketListener
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class KlageVedtakMottak(
    rapidsConnection: RapidsConnection,
) : PacketListener {
    companion object {
        const val KILDE = "DAGPENGER"
        const val EVENT_NAME = "KlageAnkeVedtak"

        fun rapidFilter(): River.() -> Unit =
            {
                precondition {
                    it.requireKey(
                        "eventId",
                        "kildeReferanse",
                        "kabalReferanse",
                        "type",
                        "detaljer",
                    )
                    it.requireValue("kilde", KILDE)
                    it.forbidValue("@event_name", EVENT_NAME)
                }
            }
    }

    init {
        River(rapidsConnection).apply(rapidFilter()).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val klageInstansEventId = packet["eventId"].asText()
        val klageId = packet["kildeReferanse"].asText()
        val klageinstansVedtakId = packet["kabalReferanse"].asText()
        val klageInstansVedtakType = packet["type"].asText()

        withLoggingContext(
            "klageId" to klageId,
            "klageinstansVedtakId" to klageinstansVedtakId,
            "klageInstansEventId" to klageInstansEventId,
            "klageInstansVedtakType" to klageInstansVedtakType,
        ) {
            logger.info { "Mottok klageAnke vedtak for behandlingId: $klageId" }
            sikkerlogg.info { "Mottok klageAnke vedtak for behandlingId: $klageId med pakke: ${packet.toJson()}" }
            packet["@event_name"] = EVENT_NAME
            context.publish(
                message = packet.toJson(),
            )
        }
    }
}
