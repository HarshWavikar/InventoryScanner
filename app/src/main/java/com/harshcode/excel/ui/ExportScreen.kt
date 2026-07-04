package com.harshcode.excel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun Counts(
    scannedCount: Int = 0,
    totalCount: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildAnnotatedString {
                append("Scanned Count: ")
                withStyle(style = SpanStyle(color = Color(0xFF4CAF50))) { // Green color
                    append("$scannedCount")
                }
            },
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = buildAnnotatedString {
                append("Total Count: ")
                withStyle(style = SpanStyle(color = Color(0xFF2196F3))) { // Blue color
                    append("$totalCount")
                }
            },
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun LabelSelection(
    labelOptions: List<String>,
    selectedLabel: String,
    onLabelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    val icon = if (expanded) {
        Icons.Default.KeyboardArrowUp
    } else {
        Icons.Default.KeyboardArrowDown
    }

    Column(modifier = modifier) {
        Box {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        textFieldSize = coordinates.size.toSize()
                    },
                value = selectedLabel,
                onValueChange = { /* Read only, handled by dropdown */ },
                label = { Text(text = "Select Label") },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { expanded = !expanded },
                        imageVector = icon,
                        contentDescription = null
                    )
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
            ) {
                labelOptions.forEach { label ->
                    DropdownMenuItem(
                        onClick = {
                            onLabelSelected(label)
                            expanded = false
                        },
                        text = {
                            Text(text = label)
                        }
                    )
                }
            }
        }
    }
}
