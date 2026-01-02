package no.nav.dagpenger.klageinstans

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

internal object Configuration {
    const val APP_NAME = "dp-kabal-integrasjon"

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to "dp-kabal-integrasjon",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-kabal-integrasjon-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_EXTRA_TOPIC" to "klage.behandling-events.v1",
                "KAFKA_RESET_POLICY" to "LATEST",
                "KLAGE_API_URL" to "http://kabal-api",
                "KLAGE_API_SCOPE" to "api://dev-gcp.todo.todo/.default",
            ),
        )

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val klageApiUrl: String = properties[Key("KLAGE_API_URL", stringType)]
    val klageApiScope: String = properties[Key("KLAGE_API_SCOPE", stringType)]
    val tokenProvider = {
        azureAdClient.clientCredentials(klageApiScope).access_token
            ?: throw RuntimeException("Klarte ikke hente token")
    }

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }
    private val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }
}
