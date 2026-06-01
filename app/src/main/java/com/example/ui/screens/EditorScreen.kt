package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.model.Note
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.markdown.MarkdownPreview
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(navController: NavController, viewModel: NoteViewModel, noteId: Int) {
    val context = LocalContext.current

    // Observe editor live state
    val editorTitle by viewModel.editorTitle.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val editorColor by viewModel.editorColor.collectAsState()
    val editorFolder by viewModel.editorFolder.collectAsState()
    val editorTags by viewModel.editorTags.collectAsState()

    var isPreviewMode by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showAiSummaryDialog by remember { mutableStateOf(false) }
    var aiSummaryResult by remember { mutableStateOf("") }
    var isLoadingAiSummary by remember { mutableStateOf(false) }

    // On-screen lists
    val foldersList by viewModel.foldersList.collectAsState()
    val uniqueTagsList by viewModel.allUniqueTags.collectAsState()

    // Load initial note details in state machine
    LaunchedEffect(noteId) {
        if (noteId != 0) {
            val note = viewModel.repository.getNoteById(noteId)
            viewModel.setEditorFields(note)
        } else {
            viewModel.setEditorFields(null)
        }
    }

    // Modern color swatch list (supporting Dark Slate default & light alternatives)
    val colorPresets = listOf(
        "#1E293B", "#312E81", "#064E3B", "#450A0A", "#4C1D95", "#431407", // Dark preserves
        "#FFF8E1", "#E8F5E9", "#E3F2FD", "#FFEBEB", "#F3E5F5", "#F1F5F9"  // Light alternatives
    )

    // Character and Word Counter
    val charCount = editorContent.length
    val wordCount = if (editorContent.isBlank()) 0 else editorContent.split("\\s+".toRegex()).size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId == 0) "Create Note" else "Edit Workspace",
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Auto save on back click
                        viewModel.createOrUpdateNoteInDb(
                            id = noteId,
                            title = editorTitle,
                            content = editorContent,
                            folder = editorFolder,
                            color = editorColor,
                            tags = editorTags
                        )
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Preview Toggle Bar
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreviewMode) "Edit Mode" else "Preview Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Undo and Redo
                    IconButton(onClick = { viewModel.performUndo() }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.performRedo() }) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }

                    // Export Trigger Options
                    var showExportMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as Markdown (.md)") },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    val dummyNote = Note(id=noteId, title=editorTitle, content=editorContent, folder=editorFolder, tags=editorTags)
                                    val file = viewModel.exportNote(dummyNote, "markdown", context)
                                    if (file != null) {
                                        Toast.makeText(context, "Exported successfully to cache: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as HTML (.html)") },
                                leadingIcon = { Icon(Icons.Default.Html, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    val dummyNote = Note(id=noteId, title=editorTitle, content=editorContent, folder=editorFolder, tags=editorTags)
                                    val file = viewModel.exportNote(dummyNote, "html", context)
                                    if (file != null) {
                                        Toast.makeText(context, "Exported successfully to cache: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Plain Text (.txt)") },
                                leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    val dummyNote = Note(id=noteId, title=editorTitle, content=editorContent, folder=editorFolder, tags=editorTags)
                                    val file = viewModel.exportNote(dummyNote, "txt", context)
                                    if (file != null) {
                                        Toast.makeText(context, "Exported successfully to cache: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }

                    // Save Option
                    IconButton(onClick = {
                        viewModel.createOrUpdateNoteInDb(
                            id = noteId,
                            title = editorTitle,
                            content = editorContent,
                            folder = editorFolder,
                            color = editorColor,
                            tags = editorTags
                        )
                        Toast.makeText(context, "Draft Saved", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Manual Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(android.graphics.Color.parseColor(editorColor))
                )
            )
        },
        containerColor = Color(android.graphics.Color.parseColor(editorColor))
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            // Note Meta Section: Workspace folder chips and background color circle trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = { showFolderDialog = true },
                        label = { Text("Folder: $editorFolder") },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, Modifier.size(14.dp)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Color Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // AI Summarizer Action Pill
                Button(
                    onClick = {
                        if (editorContent.isBlank()) {
                            Toast.makeText(context, "Note contents are empty. Add details to synthesize.", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoadingAiSummary = true
                            showAiSummaryDialog = true
                            viewModel.summarizeNoteWithAi(editorContent, editorTitle) { summary ->
                                aiSummaryResult = summary
                                isLoadingAiSummary = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Summarize", Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Summary", fontSize = 12.sp, color = Color.White)
                }
            }

            // Expandable dynamic color presets drawer slider
            AnimatedVisibility(visible = showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorPresets.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .border(
                                    width = if (editorColor == colorHex) 3.dp else 1.dp,
                                    color = if (editorColor == colorHex) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.onColorChanged(colorHex) }
                        )
                    }
                }
            }

            // Note tag line input
            OutlinedTextField(
                value = editorTags,
                onValueChange = { viewModel.onTagsChanged(it) },
                placeholder = { Text("Add labels (comma-separated, e.g., work, draft)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("tag_label_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            // Split View / Alternator Workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isPreviewMode) {
                    // Full rendered preview screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = editorTitle.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        MarkdownPreview(markdown = editorContent)
                    }
                } else {
                    // Text Editor Screen
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Title
                        TextField(
                            value = editorTitle,
                            onValueChange = { viewModel.onTitleChanged(it) },
                            placeholder = { Text("Note Title", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_title_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = if (isColorLight(editorColor)) Color.Black else Color.White)
                        )

                        // Rich editor content space
                        TextField(
                            value = editorContent,
                            onValueChange = { viewModel.onContentChanged(it) },
                            placeholder = { Text("Let your mind run free (Mark down supported)...", color = if (isColorLight(editorColor)) Color.Black.copy(alpha=0.6f) else Color.White.copy(alpha=0.6f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("note_content_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (isColorLight(editorColor)) Color.Black else Color.White),
                            maxLines = Int.MAX_VALUE
                        )
                    }
                }
            }

            // Word count and editor helper bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Column {
                    // Markdown Action Shortcut Bar (helps formatting instantly)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShortcutButton(label = "H1") { viewModel.onContentChanged(editorContent + "\n# ") }
                        ShortcutButton(label = "H2") { viewModel.onContentChanged(editorContent + "\n## ") }
                        ShortcutButton(label = "H3") { viewModel.onContentChanged(editorContent + "\n### ") }
                        ShortcutButton(label = "**Bold**") { viewModel.onContentChanged(editorContent + " **text** ") }
                        ShortcutButton(label = "*Italic*") { viewModel.onContentChanged(editorContent + " *text* ") }
                        ShortcutButton(label = "~~Strike~~") { viewModel.onContentChanged(editorContent + " ~~text~~ ") }
                        ShortcutButton(label = "`Code`") { viewModel.onContentChanged(editorContent + " `code` ") }
                        ShortcutButton(label = "> Quote") { viewModel.onContentChanged(editorContent + "\n> ") }
                        ShortcutButton(label = "- List") { viewModel.onContentChanged(editorContent + "\n- ") }

                        // Simulated Voice recognition dictation tool
                        IconButton(
                            onClick = {
                                viewModel.onContentChanged(editorContent + " [Voice dictation: Today, I am drafting smart project plans with live Cloud back up and markdown previewing] ")
                                Toast.makeText(context, "Voice memo dictated", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Dictation Tool")
                        }
                    }

                    // Word and character stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount words  |  $charCount characters",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Draft Autosaved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Modal Folder picker Dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Assign Workspace Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    foldersList.filter { it != "All" }.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onFolderChanged(f)
                                    showFolderDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = f, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal AI neural Summary dialog
    if (showAiSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showAiSummaryDialog = false },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Gemini AI Note Summary") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isLoadingAiSummary) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Querying Gemini 3.5 Neural Engine...", fontSize = 13.sp)
                    } else {
                        Column {
                            Text(
                                text = "Below is the neural bullet-point index representing your notes' focal areas:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = aiSummaryResult,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onContentChanged(editorContent + "\n\n### AI Neural Index\n" + aiSummaryResult)
                        showAiSummaryDialog = false
                        Toast.makeText(context, "Summary inserted into notes body", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isLoadingAiSummary
                ) {
                    Text("Append Summary")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiSummaryDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun ShortcutButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
