package com.kiran.financetracker

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private val preferences by lazy {
        getSharedPreferences("finance_data", MODE_PRIVATE)
    }

    private val incomeCategories =
        arrayOf("💼 Salary", "📈 Stocks", "🏦 MIS", "💰 Other")

    private val expenseCategories =
        arrayOf(
            "🛒 Groceries",
            "🚗 Transport",
            "🍔 Food",
            "🏠 Rent",
            "💡 Bills",
            "🛍 Shopping",
            "🏥 Medical",
            "📦 Other"
        )

    private val colors = listOf(
        Color.rgb(35, 105, 190),
        Color.rgb(35, 160, 90),
        Color.rgb(238, 145, 45),
        Color.rgb(150, 85, 180),
        Color.rgb(220, 75, 70),
        Color.rgb(60, 170, 175),
        Color.rgb(120, 120, 120),
        Color.rgb(220, 180, 50)
    )

    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
    }

    private fun showDashboard() {
        val root = baseLayout()

        root.addView(title("💰 Finance Tracker"))
        root.addView(subtitle("Your personal finance overview"))

        val months = arrayOf(
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        )

        val monthSpinner = Spinner(this)
        monthSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            months
        )
        monthSpinner.setSelection(selectedMonth)

        monthSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedMonth = position
                    showDashboard()
                }
            }

        root.addView(label("📅 Select month"))
        root.addView(monthSpinner)

        val transactions = filteredTransactions()

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

        root.addView(
            summaryCard(
                "💵 CURRENT BALANCE",
                "₹${money(income - expenses)}",
                Color.rgb(30, 105, 175)
            )
        )

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        row.addView(
            summaryCard(
                "💰 EARNINGS",
                "₹${money(income)}",
                Color.rgb(35, 150, 85)
            ),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                setMargins(0, 12, 6, 0)
            }
        )

        row.addView(
            summaryCard(
                "💸 EXPENSES",
                "₹${money(expenses)}",
                Color.rgb(220, 85, 70)
            ),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                setMargins(6, 12, 0, 0)
            }
        )

        root.addView(row)

        root.addView(sectionTitle("📊 Income breakdown"))

        val incomeChart = PieChartView(this)
        val incomeData = incomeByCategory(transactions)

        incomeChart.setChartData(
            incomeData.values.toList(),
            incomeData.keys.toList(),
            colors,
            "Income"
        )

        root.addView(
            incomeChart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                360
            )
        )

        addLegend(root, incomeData)

        root.addView(sectionTitle("💸 Expense breakdown"))

        val expenseChart = PieChartView(this)
        val expenseData = expenseByCategory(transactions)

        expenseChart.setChartData(
            expenseData.values.toList(),
            expenseData.keys.toList(),
            colors,
            "Expenses"
        )

        root.addView(
            expenseChart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                360
            )
        )

        addLegend(root, expenseData)

        root.addView(
            button("＋ ADD TRANSACTION") {
                showAddTransaction()
            }
        )

        root.addView(
            button("📊 REPORTS, TRACKERS & LIMITS") {
                showReports()
            }
        )

        root.addView(
            button("☷ TRANSACTION HISTORY") {
                showHistory()
            }
        )

        setContentView(root)
    }

    private fun showReports() {
        val root = baseLayout()

        root.addView(title("📊 Reports & Trackers"))
        root.addView(subtitle("Tap any section for details"))

        val transactions = filteredTransactions()

        val grocerySpent = categoryTotal(transactions, "🛒 Groceries")
        val groceryLimit = preferences.getFloat("grocery_limit", 8000f).toDouble()
        val groceryRemaining = groceryLimit - grocerySpent

        root.addView(sectionTitle("🛒 Grocery budget"))

        val groceryChart = PieChartView(this)
        groceryChart.setChartData(
            listOf(
                grocerySpent,
                maxOf(groceryRemaining, 0.0)
            ),
            listOf("Spent", "Remaining"),
            listOf(
                Color.rgb(220, 85, 70),
                Color.rgb(210, 225, 235)
            ),
            "${percent(grocerySpent, groceryLimit)}%"
        )

        root.addView(
            groceryChart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                330
            )
        )

        root.addView(infoText(
            "Spent: ₹${money(grocerySpent)}\n" +
                    "Limit: ₹${money(groceryLimit)}\n" +
                    if (groceryRemaining >= 0) {
                        "Remaining: ₹${money(groceryRemaining)}"
                    } else {
                        "Over budget: ₹${money(-groceryRemaining)}"
                    }
        ))

        root.addView(button("✏ SET GROCERY LIMIT") {
            setGroceryLimit()
        })

        root.addView(sectionTitle("💼 Salary tracker"))

        val salaryTotal = categoryTotal(transactions, "💼 Salary")
        val otherIncome = incomeByCategory(transactions)
            .filterKeys { it != "💼 Salary" }
            .values
            .sum()

        val salaryChart = PieChartView(this)
        salaryChart.setChartData(
            listOf(salaryTotal, otherIncome),
            listOf("Salary", "Other income"),
            listOf(
                Color.rgb(35, 150, 85),
                Color.rgb(35, 105, 190)
            ),
            "Income"
        )

        root.addView(
            salaryChart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                330
            )
        )

        root.addView(infoText(
            "Salary this month: ₹${money(salaryTotal)}\n" +
                    "Salary status: " +
                    if (salaryTotal > 0) "✅ Received" else "⏳ Pending"
        ))

        root.addView(sectionTitle("📈 Monthly income comparison"))

        val salaryMonths = monthSalaryTotals()
        addLegend(root, salaryMonths)

        root.addView(button("← BACK TO DASHBOARD") {
            showDashboard()
        })

        setContentView(root)
    }

    private fun showAddTransaction() {
        val root = baseLayout()

        root.addView(title("＋ Add transaction"))

        val typeSpinner = Spinner(this)
        typeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Earning", "Expense")
        )

        root.addView(label("Transaction type"))
        root.addView(typeSpinner)

        val categorySpinner = Spinner(this)

        fun loadCategories() {
            val list =
                if (typeSpinner.selectedItemPosition == 0) {
                    incomeCategories
                } else {
                    expenseCategories
                }

            categorySpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                list
            )
        }

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

        root.addView(label("Category"))
        root.addView(categorySpinner)

        val amount = EditText(this)
        amount.hint = "Amount"
        amount.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        root.addView(label("Amount"))
        root.addView(amount)

        val note = EditText(this)
        note.hint = "Optional note"

        root.addView(label("Note"))
        root.addView(note)

        root.addView(button("SAVE TRANSACTION") {
            val value = amount.text.toString().toDoubleOrNull()

            if (value == null || value <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@button
            }

            val item = JSONObject()
            item.put(
                "type",
                if (typeSpinner.selectedItemPosition == 0) "Earning"
                else "Expense"
            )
            item.put("category", categorySpinner.selectedItem.toString())
            item.put("amount", value)
            item.put("note", note.text.toString())
            item.put("timestamp", System.currentTimeMillis())

            val data = readTransactions()
            data.put(item)
            saveTransactions(data)

            Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
            showDashboard()
        })

        root.addView(button("← BACK") {
            showDashboard()
        })

        setContentView(root)
        loadCategories()
    }

    private fun showHistory() {
        val root = baseLayout()

        root.addView(title("☷ Transaction history"))

        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL

        val transactions = filteredTransactions()

        for (i in transactions.length() - 1 downTo 0) {
            val item = transactions.getJSONObject(i)
            val type = item.optString("type")
            val category = item.optString("category")
            val amount = item.optDouble("amount")
            val note = item.optString("note")

            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.setPadding(18, 16, 18, 16)
            box.setBackgroundColor(
                if (type == "Earning") {
                    Color.rgb(235, 249, 238)
                } else {
                    Color.rgb(255, 240, 238)
                }
            )

            box.addView(infoText(
                "$category\n" +
                        "$type: ₹${money(amount)}\n" +
                        if (note.isBlank()) "" else "Note: $note"
            ))

            list.addView(
                box,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    -2
                ).apply {
                    setMargins(0, 0, 0, 12)
                }
            )
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

        root.addView(button("← BACK TO DASHBOARD") {
            showDashboard()
        })

        setContentView(root)
    }

    private fun setGroceryLimit() {
        val input = EditText(this)
        input.hint = "Monthly grocery limit"
        input.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(this)
            .setTitle("🛒 Set grocery limit")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().toFloatOrNull()

                if (value != null && value > 0) {
                    preferences.edit()
                        .putFloat("grocery_limit", value)
                        .apply()

                    showReports()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filteredTransactions(): JSONArray {
        val all = readTransactions()
        val result = JSONArray()

        for (i in 0 until all.length()) {
            val item = all.getJSONObject(i)
            val timestamp = item.optLong("timestamp", 0L)

            if (timestamp == 0L || sameMonth(timestamp)) {
                result.put(item)
            }
        }

        return result
    }

    private fun sameMonth(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        return calendar.get(Calendar.MONTH) == selectedMonth &&
                calendar.get(Calendar.YEAR) == selectedYear
    }

    private fun incomeByCategory(data: JSONArray): LinkedHashMap<String, Double> {
        val result = LinkedHashMap<String, Double>()

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)

            if (item.optString("type") == "Earning") {
                val category = item.optString("category")
                result[category] =
                    (result[category] ?: 0.0) + item.optDouble("amount")
            }
        }

        return result
    }

    private fun expenseByCategory(data: JSONArray): LinkedHashMap<String, Double> {
        val result = LinkedHashMap<String, Double>()

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)

            if (item.optString("type") == "Expense") {
                val category = item.optString("category")
                result[category] =
                    (result[category] ?: 0.0) + item.optDouble("amount")
            }
        }

        return result
    }

    private fun categoryTotal(data: JSONArray, category: String): Double {
        return expenseByCategory(data)[category] ?: incomeByCategory(data)[category] ?: 0.0
    }

    private fun monthSalaryTotals(): LinkedHashMap<String, Double> {
        val result = LinkedHashMap<String, Double>()
        val all = readTransactions()

        for (i in 0 until all.length()) {
            val item = all.getJSONObject(i)

            if (item.optString("category") == "💼 Salary") {
                val timestamp = item.optLong("timestamp", 0L)

                if (timestamp > 0) {
                    val month = SimpleDateFormat(
                        "MMM yyyy",
                        Locale.getDefault()
                    ).format(Date(timestamp))

                    result[month] =
                        (result[month] ?: 0.0) + item.optDouble("amount")
                }
            }
        }

        return result
    }

    private fun addLegend(
        root: LinearLayout,
        data: Map<String, Double>
    ) {
        data.entries.forEachIndexed { index, entry ->
            root.addView(infoText(
                "● ${entry.key}    ₹${money(entry.value)}"
            ).apply {
                setTextColor(colors[index % colors.size])
            })
        }
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
            setBackgroundColor(Color.rgb(248, 250, 253))
        }
    }

    private fun title(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 27f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(25, 55, 90))
            setPadding(0, 8, 0, 4)
        }
    }

    private fun subtitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 15)
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 21f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(45, 55, 70))
            setPadding(0, 22, 0, 8)
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 9, 0, 2)
        }
    }

    private fun infoText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.rgb(55, 65, 75))
            setPadding(0, 7, 0, 7)
        }
    }

    private fun summaryCard(
        heading: String,
        value: String,
        color: Int
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(color)

            addView(TextView(this@MainActivity).apply {
                text = heading
                textSize = 13f
                setTextColor(Color.WHITE)
            })

            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 27f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, 5, 0, 0)
            })
        }
    }

    private fun button(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 15f
            setOnClickListener { action() }
        }
    }

    private fun money(value: Double): String {
        return String.format(Locale.getDefault(), "%.2f", value)
    }

    private fun percent(value: Double, total: Double): Int {
        if (total <= 0) return 0
        return ((value / total) * 100).roundToInt()
    }
}
