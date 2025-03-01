package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.model.PaymentTypes
import com.mercadopago.android.px.model.internal.Application
import com.mercadopago.android.px.model.internal.OneTapItem

internal abstract class CustomOptionIdSolver {
    abstract operator fun get(oneTapItem: OneTapItem): String

    companion object {

        @JvmStatic
        fun defaultCustomOptionId(oneTapItem: OneTapItem): String {
            return with(oneTapItem) {
                if (isCard) card.id else paymentMethodId
            }
        }

        @JvmStatic
        fun getByApplication(oneTapItem: OneTapItem, application: Application): String {
            return when {
                PaymentTypes.isCardPaymentType(application.paymentMethod.type) && oneTapItem.isCard -> oneTapItem.card.id
                else -> application.paymentMethod.id
            }
        }

        fun compare(oneTapItem: OneTapItem, customOptionId: String): Boolean {
            return with(oneTapItem) {
                (isCard && card.id == customOptionId)
                    || paymentMethodId == customOptionId
                    || compareWithOfflineMethod(oneTapItem, customOptionId)
            }
        }

        private fun compareWithOfflineMethod(oneTapItem: OneTapItem, customOptionId: String): Boolean {
            return (oneTapItem.isOfflineMethods && oneTapItem.getApplications().find {
                customOptionId == it.paymentMethod.id
            } != null)
        }
    }
}