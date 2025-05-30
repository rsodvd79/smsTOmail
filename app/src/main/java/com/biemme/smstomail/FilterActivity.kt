package com.biemme.smstomail

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.biemme.smstomail.ui.theme.SmsTOmailTheme

class FilterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilterScreen()
        }
    }
}

@Preview(showBackground = true, name = "FilterScreen Preview")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreenPreview() {
    SmsTOmailTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val mockFilters = listOf(
                Filter(
                    id = 1,
                    sender = "+39123456789",
                    keyword = "promozione",
                    filterType = FilterType.EXCLUDE
                ),
                Filter(
                    id = 2,
                    sender = "",
                    keyword = "urgente",
                    filterType = FilterType.INCLUDE
                ),
                Filter(
                    id = 3,
                    sender = "InfoSMS",
                    keyword = "",
                    filterType = FilterType.INCLUDE
                )
            )

            FilterScreenContent(
                filters = mockFilters,
                newKeyword = "esempio",
                newSender = "",
                filterType = FilterType.INCLUDE,
                onNewKeywordChange = {},
                onNewSenderChange = {},
                onFilterTypeChange = {},
                onAddFilter = {},
                onDeleteFilter = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreenContent(
    filters: List<Filter>,
    newKeyword: String,
    newSender: String,
    filterType: FilterType,
    onNewKeywordChange: (String) -> Unit,
    onNewSenderChange: (String) -> Unit,
    onFilterTypeChange: (FilterType) -> Unit,
    onAddFilter: () -> Unit,
    onDeleteFilter: (Filter) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gestione Filtri SMS") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? Activity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Spacer(Modifier.height(8.dp))

            // Campi per il mittente
            Text("Mittente (opzionale):", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = newSender,
                onValueChange = onNewSenderChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Inserisci il mittente da filtrare") }
            )

            Spacer(Modifier.height(8.dp))

            // Campi per la parola chiave
            Text("Parola chiave:", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = newKeyword,
                onValueChange = onNewKeywordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Inserisci la parola chiave da cercare") }
            )

            Spacer(Modifier.height(8.dp))

            // Selezione del tipo di filtro
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = filterType == FilterType.INCLUDE,
                    onClick = { onFilterTypeChange(FilterType.INCLUDE) }
                )
                Text("Includi", Modifier.padding(start = 4.dp))

                Spacer(Modifier.width(16.dp))

                RadioButton(
                    selected = filterType == FilterType.EXCLUDE,
                    onClick = { onFilterTypeChange(FilterType.EXCLUDE) }
                )
                Text("Escludi", Modifier.padding(start = 4.dp))
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAddFilter,
                modifier = Modifier.align(Alignment.End),
                enabled = newKeyword.isNotBlank() || newSender.isNotBlank()
            ) {
                Text("Aggiungi Filtro")
            }

            Spacer(Modifier.height(16.dp))
            Text("Filtri attivi:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(filters.size) { idx ->
                    val filter = filters[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when(filter.filterType) {
                                        FilterType.INCLUDE -> "👁️ Includi"
                                        FilterType.EXCLUDE -> "❌ Escludi"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (filter.sender.isNotEmpty()) {
                                    Text("Da: ${filter.sender}")
                                }
                                if (filter.keyword.isNotEmpty()) {
                                    Text("Contiene: ${filter.keyword}")
                                }
                            }
                            IconButton(onClick = { onDeleteFilter(filter) }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Elimina")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen() {
    val context = LocalContext.current
    var filters by remember { mutableStateOf(listOf<Filter>()) }
    var newKeyword by remember { mutableStateOf("") }
    var newSender by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(FilterType.INCLUDE) }
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        filters = db.filterDao().getAllFilters()
    }

    FilterScreenContent(
        filters = filters,
        newKeyword = newKeyword,
        newSender = newSender,
        filterType = filterType,
        onNewKeywordChange = { newKeyword = it },
        onNewSenderChange = { newSender = it },
        onFilterTypeChange = { filterType = it },
        onAddFilter = {
            if (newKeyword.isNotBlank() || newSender.isNotBlank()) {
                scope.launch {
                    db.filterDao().insertFilter(
                        Filter(
                            sender = newSender,
                            keyword = newKeyword,
                            filterType = filterType
                        )
                    )
                    filters = db.filterDao().getAllFilters()
                    newKeyword = ""
                    newSender = ""
                    filterType = FilterType.INCLUDE
                }
            }
        },
        onDeleteFilter = { filter ->
            scope.launch {
                db.filterDao().deleteFilter(filter)
                filters = db.filterDao().getAllFilters()
            }
        }
    )
}
