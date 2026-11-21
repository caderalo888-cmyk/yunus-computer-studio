package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class InvoiceLineItem(
    val description: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

object InvoiceItemCodec {
    fun encode(items: List<InvoiceLineItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("description", item.description)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                }
            )
        }
        return arr.toString()
    }

    fun decode(json: String): List<InvoiceLineItem> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                InvoiceLineItem(
                    description = obj.optString("description", ""),
                    quantity = obj.optInt("quantity", 1),
                    unitPrice = obj.optDouble("unitPrice", 0.0)
                )
            }
        }.getOrDefault(emptyList())
    }
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val itemsJson: String,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paidAmount: Double,
    val paymentMethod: String = "Cash",
    val status: String = "PAID",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val title: String,
    val amount: Double,
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "studio_projects")
data class StudioProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleType: String,
    val title: String,
    val payloadJson: String,
    val previewPath: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
