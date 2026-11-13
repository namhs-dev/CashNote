package com.namhs.cashnote.data.remote.dto

import java.math.BigDecimal
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateDtoTest {
    // NetworkModule의 Json 설정과 같게 둔다
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesSuccessResponse() {
        // 실제 API 응답 형식(2026-11-13)
        val body = """
            [{"result":1,"cur_unit":"BHD","ttb":"3,497.94","tts":"3,568.61",
            "deal_bas_r":"3,533.28","bkpr":"3,533","yy_efee_r":"0","ten_dd_efee_r":"0",
            "kftc_bkpr":"3,533","kftc_deal_bas_r":"3,533.28","cur_nm":"바레인 디나르"}]
        """.trimIndent()

        val dto = json.decodeFromString<List<ExchangeRateDto>>(body).single()

        assertEquals(1, dto.result)
        assertEquals("BHD", dto.currencyUnit)
        assertEquals("바레인 디나르", dto.currencyName)
        assertEquals(BigDecimal("3533.28"), dto.dealBaseRate?.toRateOrNull())
        assertEquals(BigDecimal("3497.94"), dto.telegraphicTransferBuying?.toRateOrNull())
        assertEquals(BigDecimal("3568.61"), dto.telegraphicTransferSelling?.toRateOrNull())
    }

    @Test
    fun parsesEmptyResponse() {
        // 영업일이 아니거나 오전 11시 이전에는 빈 배열이 온다
        assertTrue(json.decodeFromString<List<ExchangeRateDto>>("[]").isEmpty())
    }

    @Test
    fun parsesErrorResponseWithMissingFields() {
        val dto = json.decodeFromString<List<ExchangeRateDto>>("""[{"result":3}]""").single()

        assertEquals(3, dto.result)
        assertNull(dto.currencyUnit)
        assertNull(dto.dealBaseRate)
    }
}
