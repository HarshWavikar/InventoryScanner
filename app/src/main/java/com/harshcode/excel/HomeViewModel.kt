package com.harshcode.excel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harshcode.excel.model.ItemModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class HomeState(
    val sheetData: List<List<String>> = emptyList(),
    val barcode: String = "",
    val labelNo: String = "",
    val uploadedFileName: String = "No file selected",
    val selectedLabel: String = "All",
    val scannedCount: Int = 0,
    val scannedBarcodes: Set<String> = emptySet(),
    val scannedItems: List<ItemModel> = emptyList(),
    val isLoading: Boolean = false,
    val isBarcodeMode: Boolean = true,
    val showBarcodeField: Boolean = true,
    val showLabelField: Boolean = false,
    val snackbarColor: Color = Color(0xFF333333)
)

class HomeViewModel : ViewModel() {
    
    var state by mutableStateOf(HomeState())
        private set

    private val _snackbarEvent = Channel<String>()
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    // Derived States: Calculated automatically when relevant state changes
    val labelOptions by derivedStateOf {
        val data = state.sheetData
        if (data.size <= 1) {
            listOf("All")
        } else {
            val dataRows = data.drop(1)
            val labels = dataRows.mapNotNull { it.getOrNull(0) }
                .distinct()
                .filter { it.isNotBlank() }
            listOf("All") + labels
        }
    }

    val filteredData by derivedStateOf {
        val data = state.sheetData
        val selected = state.selectedLabel
        if (data.isEmpty()) return@derivedStateOf emptyList<List<String>>()
        if (selected == "All") return@derivedStateOf data

        val header = data.first()
        val rows = data.drop(1).filter { it.getOrNull(0) == selected }
        listOf(header) + rows
    }

    fun onLabelSelected(label: String) {
        state = state.copy(selectedLabel = label)
        if (label == "All" && state.sheetData.isNotEmpty()) {
            state = state.copy(snackbarColor = Color(0xFF333333))
            viewModelScope.launch {
                _snackbarEvent.send("Please select a specific label to start scanning.")
            }
        }
    }

    fun onBarcodeChange(newBarcode: String) {
        if (newBarcode.length <= 15 && newBarcode.all { it.isDigit() }) {
            state = state.copy(barcode = newBarcode)
            if (newBarcode.isNotBlank()) {
                performBarcodeSearch(autoSearch = true)
            }
        }
    }

    fun onLabelNoChange(newLabelNo: String) {
        if (newLabelNo.length <= 15) {
            state = state.copy(labelNo = newLabelNo)
        }
    }

    fun toggleMode() {
        val nextMode = !state.isBarcodeMode
        viewModelScope.launch {
            state = state.copy(showBarcodeField = false, showLabelField = false, isBarcodeMode = nextMode)
            delay(400)
            state = if (nextMode) {
                state.copy(showBarcodeField = true)
            } else {
                state.copy(showLabelField = true)
            }
        }
    }

    fun updateSheetData(data: List<List<String>>, fileName: String) {
        state = state.copy(
            sheetData = data,
            uploadedFileName = fileName,
            selectedLabel = "All",
            scannedCount = 0,
            scannedBarcodes = emptySet(),
            scannedItems = emptyList(),
            isLoading = false
        )
    }

    fun updateLoadingState(loading: Boolean) {
        state = state.copy(isLoading = loading)
    }

    fun performBarcodeSearch(autoSearch: Boolean = false) {
        val currentBarcode = state.barcode
        if (currentBarcode.isNotBlank()) {
            if (state.selectedLabel == "All") {
                if (!autoSearch) showError("Please select a specific label before scanning.")
                if (!autoSearch) state = state.copy(barcode = "")
                return
            }

            if (state.scannedBarcodes.contains(currentBarcode)) {
                showError("Duplicate barcode scanned: $currentBarcode")
                state = state.copy(barcode = "")
                return
            }

            val matchingRow = state.sheetData.drop(1).find { it.getOrNull(2) == currentBarcode }
            if (matchingRow != null) {
                val itemLabel = matchingRow.getOrNull(0) ?: ""
                if (itemLabel != state.selectedLabel) {
                    showError("Item belongs to '$itemLabel', but you selected '${state.selectedLabel}'.")
                    state = state.copy(barcode = "")
                    return
                }

                val item = ItemModel(
                    labelName = matchingRow.getOrNull(0) ?: "",
                    labelNo = matchingRow.getOrNull(1) ?: "",
                    barcodeNo = matchingRow.getOrNull(2) ?: "",
                    carat = matchingRow.getOrNull(3) ?: "",
                    grossWt = matchingRow.getOrNull(4) ?: "",
                    netWt = matchingRow.getOrNull(5) ?: "",
                    pcs = matchingRow.getOrNull(6) ?: ""
                )

                state = state.copy(
                    sheetData = state.sheetData.filter { it != matchingRow },
                    scannedCount = state.scannedCount + 1,
                    scannedBarcodes = state.scannedBarcodes + currentBarcode,
                    scannedItems = state.scannedItems + item,
                    snackbarColor = Color(0xFF4CAF50),
                    barcode = ""
                )
                viewModelScope.launch {
                    _snackbarEvent.send("Item '${item.labelName}' scanned successfully")
                }
            } else if (!autoSearch) {
                showError("No barcode found in document: $currentBarcode")
                state = state.copy(barcode = "")
            }
        }
    }

    fun performLabelNoSearch() {
        val query = state.labelNo.trim()
        if (query.isNotBlank()) {
            if (state.selectedLabel == "All") {
                showError("Please select a specific label first.")
                state = state.copy(labelNo = "")
                return
            }

            val matchingRow = state.sheetData.drop(1).find { it.getOrNull(1) == query }
            if (matchingRow != null) {
                val itemLabel = matchingRow.getOrNull(0) ?: ""
                if (itemLabel != state.selectedLabel) {
                    showError("Item belongs to '$itemLabel', but you selected '${state.selectedLabel}'.")
                    state = state.copy(labelNo = "")
                    return
                }

                val item = ItemModel(
                    labelName = matchingRow.getOrNull(0) ?: "",
                    labelNo = matchingRow.getOrNull(1) ?: "",
                    barcodeNo = matchingRow.getOrNull(2) ?: "",
                    carat = matchingRow.getOrNull(3) ?: "",
                    grossWt = matchingRow.getOrNull(4) ?: "",
                    netWt = matchingRow.getOrNull(5) ?: "",
                    pcs = matchingRow.getOrNull(6) ?: ""
                )

                state = state.copy(
                    sheetData = state.sheetData.filter { it != matchingRow },
                    scannedCount = state.scannedCount + 1,
                    scannedBarcodes = state.scannedBarcodes + item.barcodeNo,
                    scannedItems = state.scannedItems + item,
                    snackbarColor = Color(0xFF4CAF50),
                    labelNo = ""
                )
                viewModelScope.launch {
                    _snackbarEvent.send("Item '${item.labelName}' found by Label No successfully")
                }
            } else {
                showError("No item found with Label No: $query")
                state = state.copy(labelNo = "")
            }
        }
    }

    private fun showError(message: String) {
        state = state.copy(snackbarColor = Color(0xFFF44336))
        viewModelScope.launch {
            _snackbarEvent.send(message)
        }
    }
}
