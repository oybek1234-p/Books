package com.uz.kitoblar.ui.controllers.payment

import com.uz.kitoblar.ui.controllers.BookType

enum class PaymentType {
    NAQD_PUL,
    CREDIT_CARD
}

enum class Currency {
    USD,
    USZ,
    RUBLE
}

class Cost(var price: Long,var type: PaymentType,var bookType: BookType)