package com.harshcode.excel

import android.net.Uri
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harshcode.excel.ui.Counts
import com.harshcode.excel.ui.LabelSelection
import com.harshcode.excel.ui.theme.BorderColor
import com.harshcode.excel.ui.theme.ButtonColor
import com.harshcode.excel.ui.theme.Surface
import com.harshcode.excel.ui.theme.TitleSurface
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val uiState = homeViewModel.state

    // Observe snackbar events from ViewModel
    LaunchedEffect(Unit) {
        homeViewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val isTextFieldEnabled = uiState.selectedLabel != "All"

    // Launcher for saving the modified Excel file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = exportXlsToUri(context, it, uiState.sheetData)
                if (success) {
                    Toast.makeText(context, "File exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileName = getFileName(context, selectedUri) ?: "imported_file.xlsx"
            scope.launch {
                homeViewModel.updateLoadingState(true)
                copyFileToInternalStorage(context, selectedUri, fileName)
                readXlsFileFromUri(context, selectedUri) { data ->
                    homeViewModel.updateSheetData(data, fileName)
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
                                    .size(80.dp)
                            )
                        }
                        Text(
                            text = "Inventory Scan",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Surface
                ),
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
                        Text(text = uiState.uploadedFileName, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonColor,
                                    contentColor = Color.White
                                ),
                                onClick = { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
                            ) {
                                Text(text = "Choose File")
                            }
                            if (uiState.sheetData.isNotEmpty() && uiState.scannedCount != 0) {
                                Button(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ButtonColor,
                                        contentColor = Color.White
                                    ),
                                    onClick = { saveFileLauncher.launch("Exported_Data.xlsx") }
                                ) {
                                    Text(text = "Export")
                                }
                            }
                        }
                    }
                }

                Counts(
                    scannedCount = uiState.scannedCount,
                    totalCount = if (homeViewModel.filteredData.size > 1) homeViewModel.filteredData.size - 1 else 0
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelSelection(
                        modifier = Modifier
                            .weight(2f)
                            .height(60.dp),
                        labelOptions = homeViewModel.labelOptions,
                        selectedLabel = uiState.selectedLabel,
                        onLabelSelected = { homeViewModel.onLabelSelected(it) }
                    )
                    Button(
                        modifier = Modifier
                            .weight(0.7f)
                            .height(60.dp)
                            .padding(top = 8.dp),
                        onClick = { homeViewModel.toggleMode() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (uiState.isBarcodeMode) "Search by\nLabel No" else "Scan\nBarcode No",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Area Container with Fixed Height to prevent shifting
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    AnimatedVisibility(
                        visible = uiState.showBarcodeField,
                        enter = slideInVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.barcode,
                            maxLines = 1,
                            enabled = isTextFieldEnabled,
                            singleLine = true,
                            onValueChange = { homeViewModel.onBarcodeChange(it) },
                            label = { Text(text = if (isTextFieldEnabled) "Scan Barcode" else "Select a Label first") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                if (uiState.barcode.isNotEmpty()) {
                                    IconButton(onClick = { homeViewModel.performBarcodeSearch() }) {
                                        Icon(Icons.Default.Search, null)
                                    }
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.showLabelField,
                        enter = slideInVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.labelNo,
                            onValueChange = { homeViewModel.onLabelNoChange(it) },
                            label = { Text(if (isTextFieldEnabled) "Enter Label No" else "Select a Label first") },
                            enabled = isTextFieldEnabled,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (uiState.labelNo.isNotEmpty()) {
                                    IconButton(onClick = { homeViewModel.performLabelNoSearch() }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search Label No")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { homeViewModel.performLabelNoSearch() })
                        )
                    }
                }

                if (uiState.isLoading) {
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
                        if (homeViewModel.filteredData.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .heightIn(max = 2000.dp)
                                    .padding(top = 16.dp)
                            ) {
                                items(homeViewModel.filteredData.size) { rowIndex ->
                                    val row = homeViewModel.filteredData[rowIndex]
                                    Row(modifier = Modifier.background(if (rowIndex == 0) Color.LightGray else Color.Transparent)) {
                                        row.forEachIndexed { colIndex, cellValue ->
                                            val cellWidth = when (colIndex) {
                                                0 -> 110; 1 -> 70; 2 -> 115; 3 -> 50; 4 -> 75; 5 -> 65; 6 -> 40
                                                else -> 100
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

                if (uiState.scannedItems.isNotEmpty()) {
                    Text(
                        text = "Recently Scanned Items",
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
                                BorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                    ) {
                        LazyColumn {
                            items(uiState.scannedItems.reversed()) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp, horizontal = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.3f)),
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
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            ) { data ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = uiState.snackbarColor),
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

suspend fun exportXlsToUri(context: android.content.Context, uri: Uri, data: List<List<String>>): Boolean {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
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

suspend fun copyFileToInternalStorage(context: android.content.Context, uri: Uri, fileName: String) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.File(context.filesDir, fileName).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e("ExcelPicker", "Error copying file: ${e.message}")
        }
    }
}

suspend fun readXlsFileFromUri(
    context: android.content.Context,
    uri: Uri,
    callback: (List<List<String>>) -> Unit
) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)
                val formatter = org.apache.poi.ss.usermodel.DataFormatter()
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
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { callback(data) }
            }
        } catch (e: Exception) {
            Log.e("ExcelReader", "Error reading workbook: ${e.message}")
        }
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
    }
    return name ?: uri.path?.substringAfterLast('/')
}
