package me.rhunk.snapenhance.ui.manager.pages.theming

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.data.DatabaseTheme
import me.rhunk.snapenhance.common.data.DatabaseThemeContent
import me.rhunk.snapenhance.common.data.ExportedTheme
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.*
import okhttp3.OkHttpClient

class ThemingRoot: Routes.Route() {
    private val reloadDispatcher = AsyncUpdateDispatcher()
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    private val titles = listOf("Installed Themes", "Catalog")
    private var currentPage by mutableIntStateOf(0)
    private val okHttpClient by lazy { OkHttpClient() }

    private fun exportTheme(theme: DatabaseTheme) {
        context.coroutineScope.launch {
            val themeJson = ExportedTheme(
                name = theme.name,
                version = theme.version ?: "",
                author = theme.author ?: "",
                content = context.database.getThemeContent(theme.id) ?: DatabaseThemeContent()
            )

            activityLauncherHelper.saveFile(theme.name.replace(" ", "_").lowercase() + ".json") { uri ->
                runCatching {
                    context.androidContext.contentResolver.openOutputStream(uri.toUri())?.use { outputStream ->
                        outputStream.write(context.gson.toJson(themeJson).toByteArray())
                        outputStream.flush()
                    }
                    context.shortToast("Theme exported successfully")
                }.onFailure {
                    context.log.error("Failed to save theme", it)
                    context.longToast("Failed to export theme! Check logs for more details")
                }
            }
        }
    }

    private fun duplicateTheme(theme: DatabaseTheme) {
        context.coroutineScope.launch {
            val themeId = context.database.addOrUpdateTheme(theme.copy(
                updateUrl = null
            ))
            context.database.setThemeContent(themeId, context.database.getThemeContent(theme.id) ?: DatabaseThemeContent())
            context.shortToast("Theme duplicated successfully")
            withContext(Dispatchers.Main) {
                reloadDispatcher.dispatch()
            }
        }
    }

    private suspend fun importTheme(content: String, url: String? = null) {
        val theme = context.gson.fromJson(content, ExportedTheme::class.java)
        val themeId = context.database.addOrUpdateTheme(
            DatabaseTheme(
                id = -1,
                enabled = false,
                name = theme.name,
                version = theme.version,
                author = theme.author,
                updateUrl = url
            )
        )
        context.database.setThemeContent(themeId, theme.content)
        context.shortToast("Theme imported successfully")
        withContext(Dispatchers.Main) {
            reloadDispatcher.dispatch()
        }
    }

    private fun importTheme() {
        activityLauncherHelper.openFile { uri ->
            context.coroutineScope.launch {
                runCatching {
                    val themeJson = context.androidContext.contentResolver.openInputStream(uri.toUri())?.bufferedReader().use {
                        it?.readText()
                    } ?: throw Exception("Failed to read file")

                    importTheme(themeJson)
                }.onFailure {
                    context.log.error("Failed to import theme", it)
                    context.longToast("Failed to import theme! Check logs for more details")
                }
            }
        }
    }

    private suspend fun importFromURL(url: String) {
        val result = okHttpClient.newCall(
            okhttp3.Request.Builder()
                .url(url)
                .build()
        ).execute()

        if (!result.isSuccessful) {
            throw Exception("Failed to fetch theme from URL ${result.message}")
        }

        importTheme(result.body.string(), url)
    }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val floatingActionButton: @Composable () -> Unit = {
        var showImportFromUrlDialog by remember { mutableStateOf(false) }

        if (showImportFromUrlDialog) {
            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showImportFromUrlDialog = false },
                title = { Text("Import theme from URL") },
                text = {
                    val focusRequester = remember { FocusRequester() }
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    LaunchedEffect(Unit) {
                        delay(100)
                        focusRequester.requestFocus()
                    }
                },
                confirmButton = {
                    Button(
                        enabled = url.isNotBlank() && !loading,
                        onClick = {
                            loading = true
                            context.coroutineScope.launch {
                                runCatching {
                                    importFromURL(url)
                                    withContext(Dispatchers.Main) {
                                        showImportFromUrlDialog = false
                                    }
                                }.onFailure {
                                    context.log.error("Failed to import theme", it)
                                    context.longToast("Failed to import theme! ${it.message}")
                                }
                                withContext(Dispatchers.Main) {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import")
                    }
                }
            )
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            when (currentPage) {
                0 -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            routes.editTheme.navigate()
                        },
                        icon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        },
                        text = {
                            Text("New theme")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtendedFloatingActionButton(
                        onClick = {
                            importTheme()
                        },
                        icon = {
                            Icon(Icons.Default.Upload, contentDescription = null)
                        },
                        text = {
                            Text("Import from file")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtendedFloatingActionButton(
                        onClick = { showImportFromUrlDialog = true },
                        icon = {
                            Icon(Icons.Default.Link, contentDescription = null)
                        },
                        text = {
                            Text("Import from URL")
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun InstalledThemes() {
        val themes = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = reloadDispatcher) {
            context.database.getThemeList()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                if (themes.isEmpty()) {
                    Text(
                        text = translation["no_themes_hint"],
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            items(themes, key = { it.id }) { theme ->
                var showSettings by remember(theme) { mutableStateOf(false) }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            routes.editTheme.navigate {
                                this["theme_id"] = theme.id.toString()
                            }
                        }
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Palette, contentDescription = null, modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                        ) {
                            Text(text = theme.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp)
                            theme.author?.takeIf { it.isNotBlank() }?.let {
                                Text(text = "by $it", lineHeight = 15.sp, fontWeight = FontWeight.Light, fontSize = 12.sp)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            var state by remember { mutableStateOf(theme.enabled) }

                            IconButton(onClick = {
                                showSettings = true
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }

                            Switch(checked = state, onCheckedChange = {
                                state = it
                                context.database.setThemeState(theme.id, it)
                            })
                        }
                    }
                }

                if (showSettings) {
                    val actionsRow = remember {
                        mapOf(
                            ("Duplicate" to Icons.Default.ContentCopy) to { duplicateTheme(theme) },
                            ("Export" to Icons.Default.Download) to { exportTheme(theme) }
                        )
                    }
                    AlertDialog(
                        onDismissRequest = { showSettings = false },
                        title = { Text("Theme settings") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                actionsRow.forEach { entry ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            showSettings = false
                                            entry.value()
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(entry.key.second, contentDescription = null, modifier = Modifier.padding(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(entry.key.first)
                                    }
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    @Composable
    private fun ThemeCatalog() {
        val installedThemes = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = reloadDispatcher) {
            context.database.getThemeList()
        }

        Text(text = "Not Implemented", modifier = Modifier.fillMaxWidth().padding(5.dp), textAlign = TextAlign.Center)
    }

    @OptIn(ExperimentalFoundationApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val pagerState = rememberPagerState { titles.size }
        currentPage = pagerState.currentPage

        Column {
            TabRow(selectedTabIndex = pagerState.currentPage, indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.pagerTabIndicatorOffset(
                        pagerState = pagerState,
                        tabPositions = tabPositions
                    )
                )
            }) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState
            ) { page ->
                when (page) {
                    0 -> InstalledThemes()
                    1 -> ThemeCatalog()
                }
            }
        }
    }
}