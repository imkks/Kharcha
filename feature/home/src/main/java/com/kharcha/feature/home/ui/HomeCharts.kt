package com.kharcha.feature.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kharcha.feature.home.viewmodel.CategoryTotal
import com.kharcha.feature.home.viewmodel.MonthlyTotal
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun CategoryDonutChart(
    categories: List<CategoryTotal>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (categories.isEmpty()) {
                Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val total = categories.sumOf { it.totalAmount }.toFloat()
                val colors =
                    listOf(
                        Color(0xFFE91E63),
                        Color(0xFF2196F3),
                        Color(0xFFFFC107),
                        Color(0xFF4CAF50),
                        Color(0xFF9C27B0),
                    )

                Canvas(modifier = Modifier.size(150.dp)) {
                    var startAngle = -90f
                    categories.take(5).forEachIndexed { index, catTotal ->
                        val sweepAngle = (catTotal.totalAmount / total) * 360f
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 40f, cap = StrokeCap.Butt),
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseBarChart(
    monthlyTotals: List<MonthlyTotal>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth().height(200.dp)) {
            if (monthlyTotals.isEmpty()) {
                Text("No data", modifier = Modifier.align(Alignment.Center))
            } else {
                val incomeEntries =
                    monthlyTotals.mapIndexed { index, total ->
                        FloatEntry(x = index.toFloat(), y = (total.income / 100.0).toFloat())
                    }
                val expenseEntries =
                    monthlyTotals.mapIndexed { index, total ->
                        FloatEntry(x = index.toFloat(), y = (total.expense / 100.0).toFloat())
                    }

                val chartEntryModel = entryModelOf(incomeEntries, expenseEntries)

                Chart(
                    chart = columnChart(),
                    model = chartEntryModel,
                    startAxis = rememberStartAxis(),
                    bottomAxis =
                        rememberBottomAxis(
                            valueFormatter = { value, _ ->
                                val idx = value.toInt()
                                if (idx >= 0 && idx < monthlyTotals.size) {
                                    monthlyTotals[idx].monthYear.takeLast(2)
                                } else {
                                    ""
                                }
                            },
                        ),
                )
            }
        }
    }
}
