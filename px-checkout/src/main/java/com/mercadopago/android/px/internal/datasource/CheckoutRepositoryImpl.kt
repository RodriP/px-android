package com.mercadopago.android.px.internal.datasource

import com.mercadopago.android.px.addons.ESCManagerBehaviour
import com.mercadopago.android.px.internal.adapters.NetworkApi
import com.mercadopago.android.px.internal.callbacks.ApiResponse
import com.mercadopago.android.px.internal.features.FeatureProvider
import com.mercadopago.android.px.internal.mappers.InitRequestBodyMapper
import com.mercadopago.android.px.internal.mappers.OneTapItemToDisabledPaymentMethodMapper
import com.mercadopago.android.px.internal.repository.*
import com.mercadopago.android.px.internal.services.CheckoutService
import com.mercadopago.android.px.internal.tracking.TrackingRepository
import com.mercadopago.android.px.model.exceptions.ApiException
import com.mercadopago.android.px.model.internal.CheckoutResponse
import com.mercadopago.android.px.model.internal.DisabledPaymentMethod
import com.mercadopago.android.px.model.internal.OneTapItem
import com.mercadopago.android.px.tracking.internal.MPTracker

internal open class CheckoutRepositoryImpl(
    val paymentSettingRepository: PaymentSettingRepository,
    val experimentsRepository: ExperimentsRepository,
    val disabledPaymentMethodRepository: DisabledPaymentMethodRepository,
    val escManagerBehaviour: ESCManagerBehaviour,
    val networkApi: NetworkApi,
    val trackingRepository: TrackingRepository,
    val tracker: MPTracker,
    val payerPaymentMethodRepository: PayerPaymentMethodRepository,
    val oneTapItemRepository: OneTapItemRepository,
    val paymentMethodRepository: PaymentMethodRepository,
    val modalRepository: ModalRepository,
    val payerComplianceRepository: PayerComplianceRepository,
    val amountConfigurationRepository: AmountConfigurationRepository,
    val discountRepository: DiscountRepository,
    val featureProvider: FeatureProvider) : CheckoutRepository {

    override suspend fun checkout(): ApiResponse<CheckoutResponse, ApiException> {
        val body = InitRequestBodyMapper(escManagerBehaviour, featureProvider, trackingRepository)
            .map(paymentSettingRepository)

        val preferenceId = paymentSettingRepository.checkoutPreferenceId
        return preferenceId?.let {
            networkApi.apiCallForResponse(CheckoutService::class.java) {
                it.checkout(preferenceId, paymentSettingRepository.privateKey, body)
            }
        } ?: run {
            networkApi.apiCallForResponse(CheckoutService::class.java) {
                it.checkout(paymentSettingRepository.privateKey, body)
            }
        }
    }

    override fun configure(checkoutResponse: CheckoutResponse) {
        if (checkoutResponse.preference != null) {
            paymentSettingRepository.configure(checkoutResponse.preference)
        }
        paymentSettingRepository.configure(checkoutResponse.site)
        paymentSettingRepository.configure(checkoutResponse.currency)
        paymentSettingRepository.configure(checkoutResponse.configuration)
        experimentsRepository.configure(checkoutResponse.experiments)
        payerPaymentMethodRepository.configure(checkoutResponse.payerPaymentMethods)
        oneTapItemRepository.configure(checkoutResponse.oneTapItems)
        paymentMethodRepository.configure(checkoutResponse.availablePaymentMethods)
        modalRepository.configure(checkoutResponse.modals)
        payerComplianceRepository.configure(checkoutResponse.payerCompliance)
        amountConfigurationRepository.configure(checkoutResponse.defaultAmountConfiguration)
        discountRepository.configure(checkoutResponse.discountsConfigurations)
        disabledPaymentMethodRepository.configure(
            OneTapItemToDisabledPaymentMethodMapper().map(checkoutResponse.oneTapItems)
        )
        tracker.setExperiments(experimentsRepository.experiments)
    }

    override fun sortByPrioritizedCardId(oneTap: List<OneTapItem>, cardId: String) {
        val disabledPaymentMethodMap: Map<PayerPaymentMethodKey, DisabledPaymentMethod> =
            disabledPaymentMethodRepository.value
        OneTapItemSorter(oneTap, disabledPaymentMethodMap)
            .setPrioritizedCardId(cardId).sort()
    }
}