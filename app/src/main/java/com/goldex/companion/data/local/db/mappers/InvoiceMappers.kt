package com.goldex.companion.data.local.db.mappers

import com.goldex.companion.data.local.db.entities.BarterInvoiceEntity
import com.goldex.companion.data.local.db.entities.ClassicInvoiceEntity
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.SettlementMethod

fun BarterInvoiceEntity.toDomain(): BarterInvoice {
    val role = try {
        CustomerRole.valueOf(customerRole)
    } catch (_: Exception) {
        CustomerRole.WHOLESALER
    }

    val method = try {
        SettlementMethod.valueOf(settlementMethod)
    } catch (_: Exception) {
        SettlementMethod.POS
    }

    return BarterInvoice(
        id = id,
        invoiceNumber = invoiceNumber,
        createdAt = createdAt,
        customer = customer,
        customerRole = role,
        debtBasis = debtBasis.takeIf { it.isNotBlank() }?.let(com.goldex.companion.model.InvoiceDebtBasis::valueOf),
        spotPrice18k = spotPrice18k,
        salesItems = salesItems,
        receivedItems = receivedItems,
        settlementMethod = method,
        cashPosAmount = cashPosAmount,
        ledgerAmount = ledgerAmount,
        posTrackingCode = posTrackingCode,
        ledgerDueDate = ledgerDueDate,
        bullionWeight = bullionWeight,
        bullionKarat = bullionKarat,
        bullionAngNumber = bullionAngNumber,
        thirdPartyCustomer = thirdPartyCustomer,
        thirdPartyInvoiceId = thirdPartyInvoiceId,
        thirdPartyInvoiceNumber = thirdPartyInvoiceNumber,
        thirdPartyTransferWeight18k = thirdPartyTransferWeight18k,
        thirdPartyTransferAmount = thirdPartyTransferAmount,
        thirdPartyTrackingCode = thirdPartyTrackingCode,
        note = note,
        payments = payments,
        syncWithLedger = syncWithLedger
    )
}

fun BarterInvoice.toEntity(): BarterInvoiceEntity {
    return BarterInvoiceEntity(
        id = id,
        invoiceNumber = invoiceNumber,
        createdAt = createdAt,
        customer = customer,
        customerRole = customerRole.name,
        debtBasis = debtBasis?.name.orEmpty(),
        spotPrice18k = spotPrice18k,
        salesItems = salesItems,
        receivedItems = receivedItems,
        settlementMethod = settlementMethod.name,
        cashPosAmount = cashPosAmount,
        ledgerAmount = ledgerAmount,
        posTrackingCode = posTrackingCode,
        ledgerDueDate = ledgerDueDate,
        bullionWeight = bullionWeight,
        bullionKarat = bullionKarat,
        bullionAngNumber = bullionAngNumber,
        thirdPartyCustomer = thirdPartyCustomer,
        thirdPartyInvoiceId = thirdPartyInvoiceId,
        thirdPartyInvoiceNumber = thirdPartyInvoiceNumber,
        thirdPartyTransferWeight18k = thirdPartyTransferWeight18k,
        thirdPartyTransferAmount = thirdPartyTransferAmount,
        thirdPartyTrackingCode = thirdPartyTrackingCode,
        note = note,
        payments = payments,
        syncWithLedger = syncWithLedger
    )
}

fun ClassicInvoiceEntity.toDomain(): Invoice {
    return Invoice(
        id = id,
        invoiceNumber = invoiceNumber,
        createdAt = createdAt,
        customer = customer,
        items = items,
        note = note
    )
}

fun Invoice.toEntity(): ClassicInvoiceEntity {
    return ClassicInvoiceEntity(
        id = id,
        invoiceNumber = invoiceNumber,
        createdAt = createdAt,
        customer = customer,
        items = items,
        note = note
    )
}
