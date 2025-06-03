package it.drhack.smstomail

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import it.drhack.smstomail.ui.theme.SmsTOmailTheme

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
                title = { Text(stringResource(R.string.filter_screen_title)) },
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
                            contentDescription = stringResource(R.string.back_button_description)
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
            Text(stringResource(R.string.sender_label), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = newSender,
                onValueChange = onNewSenderChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.sender_placeholder)) },
                supportingText = {
                    if (newSender == "*") {
                        Text(stringResource(R.string.wildcard_explanation_sender))
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            // Campi per la parola chiave
            Text(stringResource(R.string.keyword_label), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = newKeyword,
                onValueChange = onNewKeywordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.keyword_placeholder)) },
                supportingText = {
                    if (newKeyword == "*") {
                        Text(stringResource(R.string.wildcard_explanation_keyword))
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            // Informazioni sull'uso dell'asterisco
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        stringResource(R.string.wildcard_info_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.wildcard_info_description),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Selezione del tipo di filtro
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = filterType == FilterType.INCLUDE,
                    onClick = { onFilterTypeChange(FilterType.INCLUDE) }
                )
                Text(stringResource(R.string.include_filter), Modifier.padding(start = 4.dp))

                Spacer(Modifier.width(16.dp))

                RadioButton(
                    selected = filterType == FilterType.EXCLUDE,
                    onClick = { onFilterTypeChange(FilterType.EXCLUDE) }
                )
                Text(stringResource(R.string.exclude_filter), Modifier.padding(start = 4.dp))
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAddFilter,
                modifier = Modifier.align(Alignment.End),
                enabled = newKeyword.isNotBlank() || newSender.isNotBlank()
            ) {
                Text(stringResource(R.string.add_filter_button))
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.active_filters_title), style = MaterialTheme.typography.titleMedium)
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
                                        FilterType.INCLUDE -> stringResource(R.string.include_filter_indicator)
                                        FilterType.EXCLUDE -> stringResource(R.string.exclude_filter_indicator)
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (filter.sender.isNotEmpty()) {
                                    val senderText = if (filter.sender == "*") {
                                        stringResource(R.string.any_sender)
                                    } else {
                                        stringResource(R.string.from_label, filter.sender)
                                    }
                                    Text(senderText)
                                }
                                if (filter.keyword.isNotEmpty()) {
                                    val keywordText = if (filter.keyword == "*") {
                                        stringResource(R.string.any_content)
                                    } else {
                                        stringResource(R.string.contains_label, filter.keyword)
                                    }
                                    Text(keywordText)
                                }
                            }
                            IconButton(onClick = { onDeleteFilter(filter) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete_button_description)
                                )
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
