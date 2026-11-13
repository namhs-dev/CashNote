package com.namhs.cashnote.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 한국수출입은행 현재환율 API 응답 항목 하나(통화 1개).
 *
 * 숫자는 "1,234.56"처럼 쉼표가 들어간 문자열로 온다. 숫자 변환은 [toRateOrNull]로 한다.
 * 오류 응답(result != 1)에서는 값이 비어 있을 수 있어 문자열 필드는 모두 nullable이다.
 */
@Serializable
data class ExchangeRateDto(
    /** 조회 결과. 1: 성공, 2: DATA 코드 오류, 3: 인증코드 오류, 4: 일일 제한 횟수 마감 */
    @SerialName("result")
    val result: Int,
    /** 통화 코드. 엔화 등은 "JPY(100)"처럼 기준 단위가 붙는다 */
    @SerialName("cur_unit")
    val currencyUnit: String? = null,
    /** 국가/통화명 */
    @SerialName("cur_nm")
    val currencyName: String? = null,
    /** 매매 기준율 */
    @SerialName("deal_bas_r")
    val dealBaseRate: String? = null,
    /** 전신환(송금) 받으실 때 */
    @SerialName("ttb")
    val telegraphicTransferBuying: String? = null,
    /** 전신환(송금) 보내실 때 */
    @SerialName("tts")
    val telegraphicTransferSelling: String? = null
)
