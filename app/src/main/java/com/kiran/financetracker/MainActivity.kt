package com.kiran.financetracker

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var content: LinearLayout
    private lateinit var typeSpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private lateinit var amountInput: EditText
    private lateinit var noteInput: EditText

    private val preferences by lazy {
        getSharedPreferences("finance_data", MODE_PRIVATE)
    }

    private val incomeCategories =
        arrayOf("Salary", "Stocks", "MIS", "Other")

    private val expenseCategories =
        arrayOf("Groceries", "Transport", "Food", "Rent", "Bills", "Shopping", "Medical", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
    }

    private fun showDashboard() {
        val root = baseLayout()

        val title = TextView(this).apply {
            text = "Finance Tracker"
            textSize = 28f
            setTextColor(Color.rgb(20, 55, 95))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 4)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Your money summary"
            textSize = 15f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 18)
        }
        root.addView(subtitle)

        val transactions = readTransactions()
        var income = 0.0
        var expenses = 0.0

        for (i in 0 until transactions.length()) {
            val item = transactions.getJSONObject(i)
            if (item.optString("type") == "Earning") {
                income += item.optDouble("amount")
            } else {
                expenses += item.optDouble("amount")
            }
        }

        val balance = income - expenses

        val balanceCard = card(Color.rgb(30, 105, 175))
        val balanceTitle = whiteText("CURRENT BALANCE", 14f)
        val balanceValue = whiteText("₹${formatAmount(balance)}", 30f)
        balanceValue.setTypeface(null, android.graphics.Typeface.BOLD)
        balanceCard.addView(balanceTitle)
        balanceCard.addView(balanceValue)
        root.addView(balanceCard)

        val summaryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 14, 0, 0)
        }

        val incomeCard = smallCard(
            "EARNINGS",
            "₹${formatAmount(income)}",
            Color.rgb(35, 150, 85)
        )

        val expenseCard = smallCard(
            "EXPENSES",
            "₹${formatAmount(expenses)}",
            Color.rgb(220, 85, 70)
        )

        summaryRow.addView(
            incomeCard,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                setMargins(0, 0, 6, 0)
            }
        )

        summaryRow.addView(
            expenseCard,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                setMargins(6, 0, 0, 0)
            }
        )

        root.addView(summaryRow)

        val chartTitle = sectionTitle("Income vs Expenses")
        root.addView(chartTitle)

        val chart = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.rgb(247, 249, 252))
        }

        addBar(chart, "Earnings", income, maxOf(income, expenses), Color.rgb(35, 150, 85))
        addBar(chart, "Expenses", expenses, maxOf(income, expenses), Color.rgb(220, 85, 70))

        root.addView(chart)

        root.addView(sectionTitle("Expense categories"))

        val categoryTotals = mutableMapOf<String, Double>()

        for (i in 0 until transactions.length()) {
            val item = transactions.getJSONObject(i)
            if (item.optString("type") == "Expense") {
                val category = item.optString("category")
                categoryTotals[category] =
                    (categoryTotals[category] ?: 0.0) + item.optDouble("amount")
            }
        }

        if (categoryTotals.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "No expenses recorded yet"
                textSize = 15f
                setTextColor(Color.GRAY)
                setPadding(0, 8, 0, 12)
            })
        } else {
            val categoryBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 12, 16, 12)
                setBackgroundColor(Color.rgb(247, 249, 252))
            }

            categoryTotals.entries
                .sortedByDescending { it.value }
                .forEach {
                    addBar(
                        categoryBox,
                        it.key,
                        it.value,
                        expenses,
                        Color.rgb(238, 145, 45)
                    )
                }

            root.addView(categoryBox)
        }

        val addButton = Button(this).apply {
            text = "+  ADD TRANSACTION"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(30, 105, 175))
            setOnClickListener { showAddTransaction() }
        }

        root.addView(
            addButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            ).apply {
                setMargins(0, 22, 0, 10)
            }
        )

        val historyButton = Button(this).apply {
            text = "VIEW TRANSACTION HISTORY"
            setOnClickListener { showHistory() }
        }
        root.addView(historyButton)

        setContentView(root)
    }

    private fun showAddTransaction() {
        val root = baseLayout()

        val title = sectionTitle("Add transaction")
        root.addView(title)

        typeSpinner = Spinner(this)
        typeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Earning", "Expense")
        )

        typeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    loadCategories()
                }
            }

        root.addView(label("Transaction type"))
        root.addView(typeSpinner)

        categorySpinner = Spinner(this)
        root.addView(label("Category"))
        root.addView(categorySpinner)

        amountInput = EditText(this).apply {
            hint = "Enter amount"
            inputType = 2 or 8192
        }
        root.addView(label("Amount"))
        root.addView(amountInput)

        noteInput = EditText(this).apply {
            hint = "Optional note"
        }
        root.addView(label("Note"))
        root.addView(noteInput)

        val saveButton = Button(this).apply {
            text = "SAVE TRANSACTION"
            setOnClickListener { saveTransaction() }
        }
        root.addView(saveButton)

        val backButton = Button(this).apply {
            text = "BACK TO DASHBOARD"
            setOnClickListener { showDashboard() }
        }
        root.addView(backButton)

        setContentView(root)
        loadCategories()
    }

    private fun showHistory() {
        val root = baseLayout()
        root.addView(sectionTitle("Transaction history"))

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val transactions = readTransactions()

        if (transactions.length() == 0) {
            list.addView(TextView(this).apply {
                text = "No transactions recorded yet"
                textSize = 16f
                setTextColor(Color.GRAY)
            })
        } else {
            for (index in transactions.length() - 1 downTo 0) {
                val item = transactions.getJSONObject(index)
                val type = item.optString("type")
                val category = item.optString("category")
                val amount = item.optDouble("amount")
                val note = item.optString("note")
                val date = item.optString("date")

                val row = card(
                    if (type == "Earning") {
                        Color.rgb(235, 249, 238)
                    } else {
                        Color.rgb(255, 240, 238)
                    }
                )

                val description = if (note.isBlank()) {
                    "$category • $date"
                } else {
                    "$category • $note\n$date"
                }

                row.addView(TextView(this).apply {
                    text = "$type: ₹${formatAmount(amount)}\n$description"
                    textSize = 16f
                    setTextColor(Color.DKGRAY)
                })

                row.addView(Button(this).apply {
                    text = "DELETE"
                    setOnClickListener {
                        deleteTransaction(index)
                        showHistory()
                    }
                })

                list.addView(row)
            }
        }

        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(Button(this).apply {
            text = "BACK TO DASHBOARD"
            setOnClickListener { showDashboard() }
        })

        setContentView(root)
    }

    private fun saveTransaction() {
        val amount = amountInput.text.toString().toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = JSONObject().apply {
            put("type", if (typeSpinner.selectedItemPosition == 0) "Earning" else "Expense")
            put("category", categorySpinner.selectedItem.toString())
            put("amount", amount)
            put("note", noteInput.text.toString())
            put(
                "date",
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            )
        }

        val transactions = readTransactions()
        transactions.put(transaction)
        saveTransactions(transactions)

        Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
        showDashboard()
    }

    private fun deleteTransaction(index: Int) {
        val oldTransactions = readTransactions()
        val newTransactions = JSONArray()

        for (i in 0 until oldTransactions.length()) {
            if (i != index) {
                newTransactions.put(oldTransactions.getJSONObject(i))
            }
        }

        saveTransactions(newTransactions)
    }

    private fun loadCategories() {
        if (!::categorySpinner.isInitialized) return

        val categories =
            if (typeSpinner.selectedItemPosition == 0) incomeCategories
            else expenseCategories

        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
    }

    private fun readTransactions(): JSONArray {
        return try {
            JSONArray(preferences.getString("transactions", "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun saveTransactions(data: JSONArray) {
        preferences.edit()
            .putString("transactions", data.toString())
            .apply()
    }

    private fun baseLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 20)
            setBackgroundColor(Color.WHITE)
        }
    }

    private fun card(color: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(color)
        }
    }

    private fun smallCard(title: String, value: String, color: Int): LinearLayout {
        val box = card(color)
        box.addView(whiteText(title, 12f))
        box.addView(whiteText(value, 20f))
        return box
    }

    private fun whiteText(value: String, size: Float): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 3, 0, 3)
        }
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 21f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(45, 55, 70))
            setPadding(0, 20, 0, 10)
        }
    }

    private fun label(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 10, 0, 2)
        }
    }

    private fun addBar(
        parent: LinearLayout,
        name: String,
        value: Double,
        maximum: Double,
        color: Int
    ) {
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        titleRow.addView(TextView(this).apply {
            text = name
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }, LinearLayout.LayoutParams(0, -2, 1f))

        titleRow.addView(TextView(this).apply {
            text = "₹${formatAmount(value)}"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.END
        })

        parent.addView(titleRow)

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        )

        progress.max = 100
        progress.progress =
            if (maximum <= 0) 0 else ((value / maximum) * 100).toInt()

        progress.progressTintList =
            android.content.res.ColorStateList.valueOf(color)

        parent.addView(
            progress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                18
            ).apply {
                setMargins(0, 2, 0, 12)
            }
        )
    }

    private fun formatAmount(value: Double): String {
        return String.format(Locale.getDefault(), "%.2f", value)
    }
}
