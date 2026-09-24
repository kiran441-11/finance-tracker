package com.kiran.financetracker

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var typeSpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private lateinit var amountInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var transactionList: LinearLayout
    private lateinit var totalIncomeText: TextView
    private lateinit var totalExpenseText: TextView
    private lateinit var balanceText: TextView

    private val preferences by lazy {
        getSharedPreferences("finance_data", MODE_PRIVATE)
    }

    private val incomeCategories = arrayOf(
        "Salary",
        "Stocks",
        "MIS",
        "Other"
    )

    private val expenseCategories = arrayOf(
        "Groceries",
        "Transport",
        "Food",
        "Rent",
        "Bills",
        "Shopping",
        "Medical",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildScreen()
        loadCategoryList()
        refreshTransactions()
    }

    private fun buildScreen() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 24, 24, 24)
        root.setBackgroundColor(Color.WHITE)

        val title = TextView(this)
        title.text = "Finance Tracker"
        title.textSize = 27f
        title.setTextColor(Color.rgb(20, 70, 120))
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 20)
        root.addView(title)

        val summaryBox = LinearLayout(this)
        summaryBox.orientation = LinearLayout.VERTICAL
        summaryBox.setPadding(20, 16, 20, 16)
        summaryBox.setBackgroundColor(Color.rgb(232, 242, 252))

        totalIncomeText = createSummaryText("Total earnings: ₹0.00")
        totalExpenseText = createSummaryText("Total expenses: ₹0.00")
        balanceText = createSummaryText("Balance: ₹0.00")

        summaryBox.addView(totalIncomeText)
        summaryBox.addView(totalExpenseText)
        summaryBox.addView(balanceText)

        root.addView(
            summaryBox,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val formTitle = TextView(this)
        formTitle.text = "Add transaction"
        formTitle.textSize = 21f
        formTitle.setTextColor(Color.DKGRAY)
        formTitle.setPadding(0, 25, 0, 10)
        root.addView(formTitle)

        typeSpinner = Spinner(this)
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Earning", "Expense")
        )
        typeSpinner.adapter = typeAdapter
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                loadCategoryList()
            }
        }
        root.addView(typeSpinner)

        categorySpinner = Spinner(this)
        root.addView(categorySpinner)

        amountInput = EditText(this)
        amountInput.hint = "Amount"
        amountInput.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        root.addView(amountInput)

        noteInput = EditText(this)
        noteInput.hint = "Note (optional)"
        root.addView(noteInput)

        val addButton = Button(this)
        addButton.text = "Add transaction"
        addButton.setOnClickListener {
            addTransaction()
        }
        root.addView(addButton)

        val historyTitle = TextView(this)
        historyTitle.text = "Transaction history"
        historyTitle.textSize = 21f
        historyTitle.setTextColor(Color.DKGRAY)
        historyTitle.setPadding(0, 20, 0, 10)
        root.addView(historyTitle)

        val scrollView = ScrollView(this)
        transactionList = LinearLayout(this)
        transactionList.orientation = LinearLayout.VERTICAL
        scrollView.addView(transactionList)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun createSummaryText(text: String): TextView {
        val view = TextView(this)
        view.text = text
        view.textSize = 18f
        view.setTextColor(Color.rgb(30, 30, 30))
        view.setPadding(0, 5, 0, 5)
        return view
    }

    private fun loadCategoryList() {
        if (!::categorySpinner.isInitialized || !::typeSpinner.isInitialized) return

        val categories = if (typeSpinner.selectedItemPosition == 0) {
            incomeCategories
        } else {
            expenseCategories
        }

        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
    }

    private fun addTransaction() {
        val amountText = amountInput.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (typeSpinner.selectedItemPosition == 0) {
            "Earning"
        } else {
            "Expense"
        }

        val category = categorySpinner.selectedItem.toString()
        val note = noteInput.text.toString().trim()
        val date = SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date())

        val transactions = readTransactions()

        val transaction = JSONObject()
        transaction.put("type", type)
        transaction.put("category", category)
        transaction.put("amount", amount)
        transaction.put("note", note)
        transaction.put("date", date)

        transactions.put(transaction)
        saveTransactions(transactions)

        amountInput.text.clear()
        noteInput.text.clear()

        refreshTransactions()

        Toast.makeText(this, "Transaction added", Toast.LENGTH_SHORT).show()
    }

    private fun readTransactions(): JSONArray {
        val savedData = preferences.getString("transactions", "[]") ?: "[]"

        return try {
            JSONArray(savedData)
        } catch (exception: Exception) {
            JSONArray()
        }
    }

    private fun saveTransactions(transactions: JSONArray) {
        preferences.edit()
            .putString("transactions", transactions.toString())
            .apply()
    }

    private fun refreshTransactions() {
        if (!::transactionList.isInitialized) return

        transactionList.removeAllViews()

        val transactions = readTransactions()
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (index in transactions.length() - 1 downTo 0) {
            val item = transactions.getJSONObject(index)
            val type = item.optString("type")
            val category = item.optString("category")
            val amount = item.optDouble("amount")
            val note = item.optString("note")
            val date = item.optString("date")

            if (type == "Earning") {
                totalIncome += amount
            } else {
                totalExpense += amount
            }

            addTransactionRow(
                index = index,
                type = type,
                category = category,
                amount = amount,
                note = note,
                date = date
            )
        }

        val balance = totalIncome - totalExpense

        totalIncomeText.text =
            "Total earnings: ₹${formatAmount(totalIncome)}"

        totalExpenseText.text =
            "Total expenses: ₹${formatAmount(totalExpense)}"

        balanceText.text =
            "Balance: ₹${formatAmount(balance)}"
    }

    private fun addTransactionRow(
        index: Int,
        type: String,
        category: String,
        amount: Double,
        note: String,
        date: String
    ) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(16, 14, 8, 14)
        row.setBackgroundColor(
            if (type == "Earning") {
                Color.rgb(235, 250, 235)
            } else {
                Color.rgb(255, 240, 240)
            }
        )

        val description = if (note.isBlank()) {
            "$category • $date"
        } else {
            "$category • $note • $date"
        }

        val text = TextView(this)
        text.text = "$type: ₹${formatAmount(amount)}\n$description"
        text.textSize = 16f
        text.setTextColor(Color.DKGRAY)

        val deleteButton = Button(this)
        deleteButton.text = "Delete"
        deleteButton.setOnClickListener {
            deleteTransaction(index)
        }

        row.addView(text)
        row.addView(deleteButton)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 12)

        transactionList.addView(row, params)
    }

    private fun deleteTransaction(index: Int) {
        val transactions = readTransactions()

        if (index >= 0 && index < transactions.length()) {
            val updatedTransactions = JSONArray()

            for (i in 0 until transactions.length()) {
                if (i != index) {
                    updatedTransactions.put(transactions.getJSONObject(i))
                }
            }

            saveTransactions(updatedTransactions)
            refreshTransactions()

            Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%.2f", amount)
    }
}
