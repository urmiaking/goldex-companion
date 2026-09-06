package com.goldex.companion.ui.feature

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.model.Customer
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeatureStateSplitTest {
    @Test
    fun customerManagerKeepsSelectionInSyncWhenDeletingSelectedCustomer() {
        val fakeStore = object : CustomerStore {
            private val list = mutableListOf<Customer>()
            override fun getCustomers(): List<Customer> = list.toList()
            override fun addCustomer(customer: Customer) { list.add(0, customer) }
            override fun updateCustomer(customer: Customer) {
                val index = list.indexOfFirst { it.id == customer.id }
                if (index >= 0) list[index] = customer
            }
            override fun deleteCustomer(id: String) { list.removeIf { it.id == id } }
        }

        val viewModel = CustomerManagerViewModel(fakeStore)
        val customer = Customer(id = "c-1", name = "رضا")

        viewModel.setCustomers(listOf(customer))
        viewModel.selectCustomer(customer)
        viewModel.deleteCustomer(customer.id)

        assertEquals(emptyList<Customer>(), viewModel.uiState.value.customerList)
        assertNull(viewModel.uiState.value.selectedCustomer)
    }
}
