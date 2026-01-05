package no.nav.dagpenger.klageinstans

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageVedtakMottakTest {
    private val testRapid = TestRapid()

    init {
        KlageVedtakMottak(testRapid)
    }

    @Test
    fun `Skal motta behandling event og publisere med @event_name`() {
        val eventId = UUID.randomUUID().toString()
        val behandlingId = "klage123"

        @Language("JSON")
        val inputMessage =
            """
            {
              "eventId": "$eventId",
              "kildeReferanse": "$behandlingId",
              "kilde": "DAGPENGER",
              "kabalReferanse": "kabal123",
              "type": "KLAGEBEHANDLING_AVSLUTTET",
              "detaljer": {
                "klagebehandlingAvsluttet": {
                  "avsluttet": "${LocalDateTime.now()}",
                  "utfall": "MEDHOLD",
                  "journalpostReferanser": ["jp1", "jp2"]
                }
              }
            }
            """.trimIndent()

        testRapid.sendTestMessage(inputMessage)

        val inspektør = testRapid.inspektør
        inspektør.size shouldBe 1

        val publishedMessage = inspektør.message(0)
        publishedMessage["@event_name"].asText() shouldBe "KlageAnkeVedtak"
        publishedMessage["eventId"].asText() shouldBe eventId
        publishedMessage["kildeReferanse"].asText() shouldBe behandlingId
        publishedMessage["kilde"].asText() shouldBe "DAGPENGER"
        publishedMessage["kabalReferanse"].asText() shouldBe "kabal123"
        publishedMessage["type"].asText() shouldBe "KLAGEBEHANDLING_AVSLUTTET"
        publishedMessage["detaljer"]["klagebehandlingAvsluttet"]["utfall"].asText() shouldBe "MEDHOLD"
    }

    @Test
    fun `Skal filtrere bort events med feil kilde`() {
        @Language("JSON")
        val inputMessage =
            """
            {
              "eventId": "${UUID.randomUUID()}",
              "kildeReferanse": "ref123",
              "kilde": "ANNEN_KILDE",
              "kabalReferanse": "kabal456",
              "type": "KLAGEBEHANDLING_AVSLUTTET",
              "detaljer": {
                "klagebehandlingAvsluttet": {
                  "avsluttet": "${LocalDateTime.now()}",
                  "utfall": "MEDHOLD",
                  "journalpostReferanser": ["jp1"]
                }
              }
            }
            """.trimIndent()

        testRapid.sendTestMessage(inputMessage)
        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `Skal filtrere bort events med @event_name som vi setter selv`() {
        @Language("JSON")
        val inputMessage =
            """
            {
              "@event_name": "${KlageVedtakMottak.EVENT_NAME}",
              "eventId": "${UUID.randomUUID()}",
              "kildeReferanse": "ref123",
              "kilde": "DAGPENGER",
              "kabalReferanse": "kabal456",
              "type": "KLAGEBEHANDLING_AVSLUTTET",
              "detaljer": {
                "klagebehandlingAvsluttet": {
                  "avsluttet": "${LocalDateTime.now()}",
                  "utfall": "MEDHOLD",
                  "journalpostReferanser": ["jp1"]
                }
              }
            }
            """.trimIndent()

        testRapid.sendTestMessage(inputMessage)
        testRapid.inspektør.size shouldBe 0
    }
}
