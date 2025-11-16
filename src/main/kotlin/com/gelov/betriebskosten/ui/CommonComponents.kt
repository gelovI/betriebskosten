package com.gelov.betriebskosten.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun ScreenTitle(
    title: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                trailing()
            }
        }
    }
}

@Composable
fun YearSelector(
    year: Int,
    onChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = year.toString(),
        onValueChange = { text ->
            text.toIntOrNull()?.let(onChange)
        },
        label = { Text("Jahr") },
        singleLine = true,
        modifier = Modifier.width(120.dp)
    )
}

@Composable
fun SimpleTable(
    headers: List<String>,
    rows: List<List<String>>,
    actions: (@Composable (rowIndex: Int) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            headers.forEach { header ->
                Text(
                    header,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        HorizontalDivider()

        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { cell ->
                    Text(
                        cell,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (actions != null) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions(index)
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun TableActionButton(
    label: String,
    color: ButtonColors,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = color,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(label, fontSize = 13.sp)
    }
}

fun BigDecimal.toEuro(withSign: Boolean = false): String {
    val scaled = this.setScale(2, RoundingMode.HALF_UP)
    val signPrefix = if (withSign && scaled != BigDecimal.ZERO) {
        if (scaled > BigDecimal.ZERO) "+" else ""
    } else ""
    val str = scaled.toPlainString().replace('.', ',')
    return "$signPrefix$str"
}

@Composable
fun HeaderCell(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.widthIn(min = 80.dp)
    )
}

@Composable
fun BodyCell(text: String) {
    Text(
        text = text,
        modifier = Modifier.widthIn(min = 80.dp)
    )
}
