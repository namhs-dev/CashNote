package com.namhs.cashnote.domain.model

/**
 * 거래의 결제수단.
 *
 * 이름(name)이 DB에 그대로 저장되므로 항목 이름을 바꾸면 migration이 필요하다.
 */
enum class PaymentMethod {
    CASH,
    CARD,

    /** 상품권·포인트·기프티콘 등. 결제수단을 기록하기 전에 저장한 거래도 여기에 든다 */
    OTHER
}
