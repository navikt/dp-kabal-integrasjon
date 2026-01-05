package no.nav.dagpenger.klageinstans

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageBehovløserTest {
    private val testRapid = TestRapid()

    val behandlingId = UUIDv7.ny().toString()
    val ident = "11111111111"
    val fagsakId = UUIDv7.ny().toString()
    val behandlendeEnhet = "4408"
    val hjemler = listOf("FTRL_4_2", "FTRL_4_9", "FTRL_4_18")
    val tilknyttedeJournalposter =
        listOf(
            Journalposter(journalpostId = "jp1", type = "BRUKERS_KLAGE"),
            Journalposter(journalpostId = "jp2", type = "OPPRINNELIG_VEDTAK"),
        )
    val prosessFullmektig =
        ProsessFullmektig(
            navn = "Djevelens Advokat",
            adresse =
                Adresse(
                    addresselinje1 = "Sydenveien 1",
                    addresselinje2 = "Poste restante",
                    addresselinje3 = "Teisen postkontor",
                    postnummer = "0666",
                    poststed = "Oslo",
                    land = "NO",
                ),
        )
    val kommentar = "kult"

    @Test
    fun `Skal løse behov dersom filter matcher`() {
        val nå = LocalDateTime.now()
        val klageKlient =
            mockk<KlageHttpKlient>().also {
                coEvery {
                    it.oversendKlageAnke(
                        behandlingId = behandlingId,
                        ident = ident,
                        fagsakId = fagsakId,
                        behandlendeEnhet = behandlendeEnhet,
                        hjemler = hjemler,
                        opprettet = nå.toLocalDate(),
                        kommentar = kommentar,
                        tilknyttedeJournalposter = tilknyttedeJournalposter,
                    )
                } returns Result.success(HttpStatusCode.OK)
            }

        KlageBehovløser(
            rapidsConnection = testRapid,
            klageKlient = klageKlient,
        )
        testRapid.sendBehov(nå)
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
                "@event_name" : "behov",
                "@behov" : [ "OversendelseKlageinstans" ],
                "behandlingId" : "$behandlingId",
                "ident" : "$ident",
                "fagsakId" : "$fagsakId",
                "behandlendeEnhet" : "$behandlendeEnhet",
                "hjemler": ["FTRL_4_2", "FTRL_4_9", "FTRL_4_18"],
                "tilknyttedeJournalposter": [
                    {
                        "journalpostId": "jp1",
                        "type": "BRUKERS_KLAGE"
                    },
                    {
                        "journalpostId": "jp2",
                        "type": "OPPRINNELIG_VEDTAK"
                    }
                ],
                "opprettet": "$nå",
                "kommentar": "$kommentar",
                "@løsning": {
                    "OversendelseKlageinstans": "OK"
                }
            }
            """.trimIndent()
    }

    @Test
    fun `Skal løse behov med fullmektig dersom filter matcher`() {
        val nå = LocalDateTime.now()
        val klageKlient =
            mockk<KlageHttpKlient>().also {
                coEvery {
                    it.oversendKlageAnke(
                        behandlingId = behandlingId,
                        ident = ident,
                        fagsakId = fagsakId,
                        behandlendeEnhet = behandlendeEnhet,
                        hjemler = hjemler,
                        prosessFullmektig = prosessFullmektig,
                        opprettet = nå.toLocalDate(),
                    )
                } returns Result.success(HttpStatusCode.OK)
            }

        KlageBehovløser(
            rapidsConnection = testRapid,
            klageKlient = klageKlient,
        )
        testRapid.sendBehovMedFullmektig(nå)
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
                "@event_name" : "behov",
                "@behov" : [ "OversendelseKlageinstans" ],
                "behandlingId" : "$behandlingId",
                "ident" : "$ident",
                "fagsakId" : "$fagsakId",
                "behandlendeEnhet" : "$behandlendeEnhet",
                "hjemler": ["FTRL_4_2", "FTRL_4_9", "FTRL_4_18"],
                "opprettet": "$nå",
                "prosessfullmektigNavn": "Djevelens Advokat",
                "prosessfullmektigAdresselinje1": "Sydenveien 1",
                "prosessfullmektigAdresselinje2": "Poste restante",
                "prosessfullmektigAdresselinje3": "Teisen postkontor",
                "prosessfullmektigPostnummer": "0666",
                "prosessfullmektigPoststed": "Oslo",
                "prosessfullmektigLand": "NO",
                "@løsning": {
                    "OversendelseKlageinstans": "OK"
                }
            }
            """.trimIndent()
    }

    @Test
    fun `Bad request fører til runtime exception`() {
        val nå = LocalDateTime.now()
        val klageKlient =
            KlageHttpKlient(
                klageApiUrl = "http://localhost:8080",
                tokenProvider = { " " },
                httpClient =
                    httpClient(
                        engine =
                            MockEngine {
                                respondBadRequest()
                            },
                    ),
            )
        KlageBehovløser(
            rapidsConnection = testRapid,
            klageKlient = klageKlient,
        )
        shouldThrow<RuntimeException> {
            testRapid.sendBehov(nå)
        }
    }

    private fun TestRapid.sendBehov(når: LocalDateTime) {
        this.sendTestMessage(
            """
            {
                "@event_name" : "behov",
                "@behov" : [ "OversendelseKlageinstans" ],
                "behandlingId" : "$behandlingId",
                "ident" : "$ident",
                "fagsakId" : "$fagsakId",
                "behandlendeEnhet" : "$behandlendeEnhet",
                "hjemler": ["FTRL_4_2", "FTRL_4_9", "FTRL_4_18"],
                "tilknyttedeJournalposter": [
                    {
                        "journalpostId": "jp1",
                        "type": "BRUKERS_KLAGE"
                    },
                    {
                        "journalpostId": "jp2",
                        "type": "OPPRINNELIG_VEDTAK"
                    }
                ],
                "opprettet": "$når",
                "kommentar": "$kommentar",
                "prosessfullmektigNavn": null,
                "prosessfullmektigAdresselinje1": null,
                "prosessfullmektigAdresselinje2": null,
                "prosessfullmektigAdresselinje3": null,
                "prosessfullmektigPostnummer": null,
                "prosessfullmektigPoststed": null,
                "prosessfullmektigLand": null
            }
            
            """.trimIndent(),
        )
    }

    private fun TestRapid.sendBehovMedFullmektig(når: LocalDateTime) {
        this.sendTestMessage(
            """
            {
                "@event_name" : "behov",
                "@behov" : [ "OversendelseKlageinstans" ],
                "behandlingId" : "$behandlingId",
                "ident" : "$ident",
                "fagsakId" : "$fagsakId",
                "behandlendeEnhet" : "$behandlendeEnhet",
                "hjemler": ["FTRL_4_2", "FTRL_4_9", "FTRL_4_18"],
                "opprettet": "$når",
                "prosessfullmektigNavn": "Djevelens Advokat",
                "prosessfullmektigAdresselinje1": "Sydenveien 1",
                "prosessfullmektigAdresselinje2": "Poste restante",
                "prosessfullmektigAdresselinje3": "Teisen postkontor",
                "prosessfullmektigPostnummer": "0666",
                "prosessfullmektigPoststed": "Oslo",
                "prosessfullmektigLand": "NO"
            }
            
            """.trimIndent(),
        )
    }
}
