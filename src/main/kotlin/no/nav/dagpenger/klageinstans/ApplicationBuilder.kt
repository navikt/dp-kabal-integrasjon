package no.nav.dagpenger.klageinstans

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val rapidsConnection: RapidsConnection = RapidApplication.create(config)

    init {
        rapidsConnection.register(this).also {
            KlageBehovløser(
                rapidsConnection = rapidsConnection,
                klageKlient =
                    KlageHttpKlient(
                        klageApiUrl = Configuration.klageApiUrl,
                        tokenProvider = Configuration.tokenProvider,
                    ),
            )
            KlageVedtakMottak(rapidsConnection = rapidsConnection)
        }
    }

    fun start() = rapidsConnection.start()

    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter opp dp-kabal-integrasjon" }
    }
}
