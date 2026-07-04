package com.harshcode.excel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harshcode.excel.model.ItemModel
import com.harshcode.excel.ui.Counts
import com.harshcode.excel.ui.LabelSelection
import com.harshcode.excel.ui.theme.ButtonColor
import com.harshcode.excel.ui.theme.Surface
import com.harshcode.excel.ui.theme.TitleSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

//    var sheetData by remember { mutableStateOf(listOf<List<String>>()) }         // Core data state: List of rows (List of Strings)
    var sheetData by rememberSaveable { mutableStateOf(listOf<List<String>>()) }         // Core data state: List of rows (List of Strings)
    var barcode by remember { mutableStateOf("") }
    var labelNo by remember { mutableStateOf("") }
    var uploadedFileName by remember { mutableStateOf("No file selected") }
    var selectedLabel by remember { mutableStateOf("All") }
    var scannedCount by remember { mutableStateOf(0) }
    var scannedBarcodes by remember { mutableStateOf(setOf<String>()) }
    var scannedItems by remember { mutableStateOf(listOf<ItemModel>()) }
    var isLoading by remember { mutableStateOf(false) }

    var isBarcodeMode by remember { mutableStateOf(true) }
    // Internal visibility states for sequenced animation
    var showBarcodeField by remember { mutableStateOf(true) }
    var showLabelField by remember { mutableStateOf(false) }

    // Sequenced animation logic
    LaunchedEffect(isBarcodeMode) {
        showBarcodeField = false
        showLabelField = false
        delay(400) // Delay to let the previous field exit completely
        if (isBarcodeMode) {
            showBarcodeField = true
        } else {
            showLabelField = true
        }
    }


    // Derived state for enabling barcode input
    val isTextFieldEnabled = selectedLabel != "All"

    // Snackbar state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarColor by remember { mutableStateOf(Color(0xFF333333)) }

    val performBarcodeSearch = {
        if (barcode.isNotBlank()) {
            if (selectedLabel == "All") {
                snackbarColor = Color(0xFFF44336) // Red
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("Please select a specific label before scanning.")
                }
                barcode = ""
            } else if (scannedBarcodes.contains(barcode)) {
                snackbarColor = Color(0xFFF44336) // Red
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("Duplicate barcode scanned: $barcode")
                }
                barcode = ""
            } else {
                val matchingRow = sheetData.drop(1).find { it.getOrNull(2) == barcode }
                if (matchingRow != null) {
                    val itemLabel = matchingRow.getOrNull(0) ?: ""
                    if (itemLabel != selectedLabel) {
                        snackbarColor = Color(0xFFF44336) // Red
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("You have selected '$selectedLabel'. Only barcodes belonging to this label can be scanned.")
                        }
                        barcode = ""
                    } else {
                        val item = ItemModel(
                            labelName = matchingRow.getOrNull(0) ?: "",
                            labelNo = matchingRow.getOrNull(1) ?: "",
                            barcodeNo = matchingRow.getOrNull(2) ?: "",
                            carat = matchingRow.getOrNull(3) ?: "",
                            grossWt = matchingRow.getOrNull(4) ?: "",
                            netWt = matchingRow.getOrNull(5) ?: "",
                            pcs = matchingRow.getOrNull(6) ?: ""
                        )
                        val itemName = item.labelName.ifBlank { "Unknown Item" }
                        sheetData = sheetData.filter { it != matchingRow }
                        scannedCount++
                        scannedBarcodes = scannedBarcodes + barcode
                        scannedItems = scannedItems + item
                        snackbarColor = Color(0xFF4CAF50) // Green
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Item '$itemName' scanned successfully")
                        }
                        barcode = ""
                    }
                } else {
                    snackbarColor = Color(0xFFF44336) // Red
                    val tempBarcode = barcode
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = "No barcode found in document: $tempBarcode",
                            duration = SnackbarDuration.Short
                        )
                    }
                    barcode = ""
                }
            }
        }
    }

    val performLabelNoSearch = {
        val query = labelNo.trim()
        if (query.isNotBlank()) {
            if (selectedLabel == "All") {
                snackbarColor = Color(0xFFF44336)
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("Please select a specific label first.")
                }
                labelNo = ""
            } else {
                val matchingRow = sheetData.drop(1).find { it.getOrNull(1) == query }
                if (matchingRow != null) {
                    val itemLabel = matchingRow.getOrNull(0) ?: ""
                    if (itemLabel != selectedLabel) {
                        snackbarColor = Color(0xFFF44336)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Item belongs to '$itemLabel', but you selected '$selectedLabel'.")
                        }
                    } else {
                        val item = ItemModel(
                            labelName = matchingRow.getOrNull(0) ?: "",
                            labelNo = matchingRow.getOrNull(1) ?: "",
                            barcodeNo = matchingRow.getOrNull(2) ?: "",
                            carat = matchingRow.getOrNull(3) ?: "",
                            grossWt = matchingRow.getOrNull(4) ?: "",
                            netWt = matchingRow.getOrNull(5) ?: "",
                            pcs = matchingRow.getOrNull(6) ?: ""
                        )
                        sheetData = sheetData.filter { it != matchingRow }
                        scannedCount++
                        scannedBarcodes = scannedBarcodes + item.barcodeNo
                        scannedItems = scannedItems + item
                        snackbarColor = Color(0xFF4CAF50)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Label No '$query' found successfully")
                        }
                    }
                    labelNo = ""
                } else {
                    snackbarColor = Color(0xFFF44336)
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Label No '$query' not found (or already scanned).")
                    }
                    labelNo = ""
                }
            }
        }
    }


// Show message when "All" is selected to prompt for specific label
    LaunchedEffect(selectedLabel) {
        if (selectedLabel == "All" && sheetData.isNotEmpty()) {
            snackbarColor = Color(0xFF333333) // Default dark gray
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("Please select a specific label to start scanning.")
        }
    }

// Launcher for saving the modified Excel file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = exportXlsToUri(context, it, sheetData)
                if (success) {
                    Toast.makeText(context, "File exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

// 1. Dynamic Unique Labels extracted from the first column of data
    val labelOptions = remember(sheetData) {
        if (sheetData.size <= 1) {
            listOf("All")
        } else {
            val dataRows = sheetData.drop(1)
            val labels = dataRows.mapNotNull { it.getOrNull(0) }
                .distinct()
                .filter { it.isNotBlank() }
            listOf("All") + labels
        }
    }

// 2. Computed Filtered Data
    val filteredData = remember(sheetData, selectedLabel) {
        if (sheetData.isEmpty()) return@remember emptyList<List<String>>()
        if (selectedLabel == "All") return@remember sheetData

        val header = sheetData.first()
        val rows = sheetData.drop(1).filter { it.getOrNull(0) == selectedLabel }
        listOf(header) + rows
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileName = getFileName(context, selectedUri) ?: "imported_file.xlsx"
            uploadedFileName = fileName

            scope.launch {
                isLoading = true
                // Copy to internal storage
                copyFileToInternalStorage(context, selectedUri, fileName)
                // Parse the file generically
                readXlsFileFromUri(context, selectedUri) { data ->
                    sheetData = data
                    selectedLabel = "All" // Reset filter on new file upload
                    scannedCount = 0 // Reset scanned count for new file
                    scannedBarcodes = emptySet() // Reset scanned barcodes for new file
                    scannedItems = emptyList() // Reset scanned items for new file
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-8).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.launcher_icon),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(45.dp)
                            )
                        }
                        Text(
                            text = "Inventory Scan",
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Surface),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .background(Surface.copy(alpha = 0.5f))
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
//                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // File Upload Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Upload Excel File",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(text = uploadedFileName, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonColor,
                                    contentColor = Color.White
                                ),
                                onClick = { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }) {
                                Text(text = "Choose File")
                            }
                            if (sheetData.isNotEmpty() && scannedCount != 0) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ButtonColor,
                                        contentColor = Color.White
                                    ),
                                    onClick = { saveFileLauncher.launch("Exported_Data.xlsx") }) {
                                    Text(text = "Export")
                                }
                            }
                        }
                    }
                }

                Counts(                                         // Statistics (Scanned/Total)
                    scannedCount = scannedCount,
                    totalCount = if (filteredData.size > 1) filteredData.size - 1 else 0
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelSelection(
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp),
                        labelOptions = labelOptions,
                        selectedLabel = selectedLabel,
                        onLabelSelected = { selectedLabel = it }
                    )
                    Button(
                        modifier = Modifier.weight(0.8f),
                        onClick = { isBarcodeMode = !isBarcodeMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isBarcodeMode) "Scan by Label No" else "Scan by Barcode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
                ) {
                    AnimatedVisibility(
                        visible = showBarcodeField,
                        enter = slideInVertically(
                            animationSpec = tween(300)
                        ) + fadeIn(
                            animationSpec = tween(300)
                        ),
                        exit = slideOutVertically(
                            animationSpec = tween(300)
                        ) + fadeOut(
                            animationSpec = tween(300)
                        )
                    ) {
                        OutlinedTextField(                               // Barcode Input
                            modifier = Modifier.fillMaxWidth(),
                            value = barcode,
                            maxLines = 1,
                            enabled = isTextFieldEnabled,
                            singleLine = true,
                            onValueChange = { input ->
                                // Limit to 15 digits and only allow digits
                                if (input.length <= 15 && input.all { it.isDigit() }) {
                                    barcode = input

                                    if (barcode.isNotBlank()) {
                                        // Extra check just in case
                                        if (selectedLabel == "All") {
                                            snackbarColor = Color(0xFFF44336) // Red
                                            scope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                snackbarHostState.showSnackbar("Please select a specific label before scanning.")
                                            }
                                            barcode = ""
                                            return@OutlinedTextField
                                        }

                                        // Check if barcode already scanned
                                        if (scannedBarcodes.contains(barcode)) {
                                            snackbarColor = Color(0xFFF44336) // Red
                                            scope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                snackbarHostState.showSnackbar("Duplicate barcode scanned: $input")
                                            }
                                            barcode = "" // Clear for next scan
                                            return@OutlinedTextField
                                        }

                                        // Logic to find row based on barcode in the entire dataset
                                        val matchingRow =
                                            sheetData.drop(1).find { it.getOrNull(2) == barcode }
                                        if (matchingRow != null) {
                                            val itemLabel = matchingRow.getOrNull(0) ?: ""

                                            // Validation: If a specific label is selected, only allow scanning items with that label
                                            if (selectedLabel != "All" && itemLabel != selectedLabel) {
                                                snackbarColor = Color(0xFFF44336) // Red
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar("You have selected '$selectedLabel'. Only barcodes belonging to this label can be scanned.")
                                                }
                                                barcode = ""
                                                return@OutlinedTextField
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

                                            val itemName = item.labelName.ifBlank { "Unknown Item" }
                                            sheetData = sheetData.filter { it != matchingRow }
                                            scannedCount++
                                            scannedBarcodes = scannedBarcodes + barcode
                                            scannedItems = scannedItems + item

                                            snackbarColor = Color(0xFF4CAF50) // Green
                                            scope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                snackbarHostState.showSnackbar("Item '$itemName' scanned successfully")
                                            }
                                            barcode = "" // Clear for next scan
                                        }
                                    }
                                }
                            },
                            label = { Text(text = if (isTextFieldEnabled) "Scan Barcode" else "Select a Label to Scan") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                if (barcode.length >= 11) {
                                    IconButton(onClick = { performBarcodeSearch() }) {
                                        Icon(Icons.Default.Search, null)
                                    }
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = showLabelField,
                        enter = slideInVertically(animationSpec = tween(300)) + fadeIn(
                            animationSpec = tween(
                                300
                            )
                        ),
                        exit = slideOutVertically(animationSpec = tween(300)) + fadeOut(
                            animationSpec = tween(
                                300
                            )
                        )
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = labelNo,
                            onValueChange = { if (it.length <= 15) labelNo = it },
                            label = { Text(if (isTextFieldEnabled) "Enter Label No" else "Select a Label first") },
                            enabled = isTextFieldEnabled,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (labelNo.isNotEmpty()) {
                                    IconButton(onClick = { performLabelNoSearch() }) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Search Label No"
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { performLabelNoSearch() })
                        )
                    }
                }

                if (isLoading) {
                    // Show Progress Indicator while fetching
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Generic Data Table
                        if (filteredData.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .heightIn(max = 2000.dp)
                                    .padding(top = 16.dp)
                            ) {
                                items(filteredData.size) { rowIndex ->
                                    val row = filteredData[rowIndex]
                                    Row(modifier = Modifier.background(if (rowIndex == 0) Color.LightGray else Color.Transparent)) {
                                        row.forEachIndexed { colIndex, cellValue ->
                                            val cellWidth = when (colIndex) {
                                                0 -> 110
                                                1 -> 70
                                                2 -> 115
                                                3 -> 50
                                                4 -> 75
                                                5 -> 65
                                                6 -> 40
                                                else -> 100 // Fallback width for index 2 and others
                                            }
                                            TableCell(
                                                text = cellValue,
                                                width = cellWidth,
                                                isHeader = (rowIndex == 0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scanned Items List
                if (scannedItems.isNotEmpty()) {
                    Text(
                        text = "Recently Scanned Items: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .align(Alignment.Start),
                        textDecoration = TextDecoration.Underline,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .border(
                                width = 2.dp,
                                Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                    ) {
                        LazyColumn {
                            items(scannedItems.reversed()) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp, horizontal = 6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Red.copy(
                                            alpha = 0.3f
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                start = 10.dp,
                                                top = 4.dp,
                                                bottom = 4.dp,
                                                end = 4.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy((-10).dp)
                                        ) {
                                            Text(
                                                text = "${item.labelName} - ${item.barcodeNo}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Gross.Wt: ${item.grossWt}g | Net.Wt: ${item.netWt}g | Carat: ${item.carat} | Pcs: ${item.pcs}",
                                                fontSize = 8.sp
                                            )
                                        }
                                        Text(
                                            text = item.labelNo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter) // This puts it at the top
                    .padding(top = 8.dp),       // Small gap from the TopBar
            ) { data ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = snackbarColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = data.visuals.message,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, width: Int, isHeader: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .background(if (isHeader) TitleSurface else ButtonColor.copy(alpha = 0.3f))
            .width(width.dp)
            .border(0.5.dp, Color.Gray)
            .padding(horizontal = 4.dp),
        fontSize = 8.sp,
        color = if (isHeader) Surface else Color.Black,
        fontWeight = if (isHeader) FontWeight.ExtraBold else FontWeight.Bold
    )
}

/**
 * Creates a new Excel workbook from sheetData and writes it to the provided Uri
 */
suspend fun exportXlsToUri(context: Context, uri: Uri, data: List<List<String>>): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Sheet1")

            data.forEachIndexed { rowIndex, rowData ->
                val row = sheet.createRow(rowIndex)
                rowData.forEachIndexed { colIndex, cellValue ->
                    val cell = row.createCell(colIndex)
                    cell.setCellValue(cellValue)
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            true
        } catch (e: Exception) {
            Log.e("ExcelExport", "Export failed: ${e.message}")
            false
        }
    }
}

suspend fun copyFileToInternalStorage(context: Context, uri: Uri, fileName: String) {
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                File(context.filesDir, fileName).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e("ExcelPicker", "Error copying file: ${e.message}")
        }
    }
}

suspend fun readXlsFileFromUri(
    context: Context,
    uri: Uri,
    callback: (List<List<String>>) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val formatter = DataFormatter()
                val data = mutableListOf<List<String>>()
                if (workbook.numberOfSheets > 0) {
                    val sheet = workbook.getSheetAt(0)
                    for (row in sheet) {
                        val rowData = mutableListOf<String>()
                        for (i in 0 until row.lastCellNum) {
                            rowData.add(formatter.formatCellValue(row.getCell(i)))
                        }
                        data.add(rowData)
                    }
                }
                workbook.close()
                withContext(Dispatchers.Main) { callback(data) }
            }
        } catch (e: Exception) {
            Log.e("ExcelReader", "Error reading workbook: ${e.message}")
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
    }
    return name ?: uri.path?.substringAfterLast('/')
}
