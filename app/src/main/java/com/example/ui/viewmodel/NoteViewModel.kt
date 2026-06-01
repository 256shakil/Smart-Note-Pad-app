package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val repository = NoteRepository(database.noteDao())
    val settingsManager = SettingsManager(context)

    // User session flows from SettingsManager
    val userEmail: StateFlow<String?> = settingsManager.userEmailFlow
    val themeMode: StateFlow<String> = settingsManager.themeFlow
    val onboardingCompleted: StateFlow<Boolean> = settingsManager.onboardingCompletedFlow
    val pinCode: StateFlow<String?> = settingsManager.pinCodeFlow
    val isLockEnabled: StateFlow<Boolean> = settingsManager.isLockedFlow
    val fontSizeScale: StateFlow<Float> = settingsManager.fontSizeFlow
    val lastSyncTime: StateFlow<Long> = settingsManager.lastSyncTimeFlow

    // Lock screen authentication state
    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked

    // Search and filtering states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFolder = MutableStateFlow("All")
    val selectedFolder: StateFlow<String> = _selectedFolder

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    // Cloud Syncing States
    private val _syncingState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncingState: StateFlow<SyncState> = _syncingState

    // Live Note Editor temporary fields (with Undo/Redo)
    private val _editorTitle = MutableStateFlow("")
    val editorTitle: StateFlow<String> = _editorTitle

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent

    private val _editorColor = MutableStateFlow("#2D3748")
    val editorColor: StateFlow<String> = _editorColor

    private val _editorFolder = MutableStateFlow("All")
    val editorFolder: StateFlow<String> = _editorFolder

    private val _editorTags = MutableStateFlow("")
    val editorTags: StateFlow<String> = _editorTags

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    init {
        // Run initial lock check if pin lock is enabled
        if (settingsManager.isLockEnabled() && settingsManager.getPinCode() != null) {
            _isAppUnlocked.value = false
        }
    }

    // Reactively filtered notes list using Flow.combine
    val activeNotesFiltered: StateFlow<List<Note>> = combine(
        repository.activeNotes,
        _searchQuery,
        _selectedFolder,
        _selectedTag
    ) { notes, query, folder, tag ->
        notes.asSequence()
            .filter { note ->
                val matchesQuery = query.isEmpty() ||
                        note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
                val matchesFolder = folder == "All" || note.folder.equals(folder, ignoreCase = true)
                val matchesTag = tag == null || note.tags.split(",").map { it.trim() }.contains(tag)
                matchesQuery && matchesFolder && matchesTag
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUniqueTags: StateFlow<List<String>> = repository.activeNotes.map { notes ->
        notes.flatMap { note ->
            note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersList: StateFlow<List<String>> = repository.folders.map { rawFolders ->
        (listOf("All", "Work", "Personal", "Ideas", "Travel") + rawFolders).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Work", "Personal", "Ideas", "Travel"))

    // Authentication Actions
    fun signInUser(email: String) {
        settingsManager.setUserEmail(email)
        triggerCloudSync()
    }

    fun signOutUser() {
        settingsManager.setUserEmail(null)
    }

    fun setOnboardingDone() {
        settingsManager.setOnboardingCompleted(true)
    }

    // App Lock PIN Logic
    fun unlockAppWithPin(pin: String): Boolean {
        val savedPin = settingsManager.getPinCode()
        return if (savedPin == pin) {
            _isAppUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun enablePinProtection(pin: String) {
        settingsManager.setPinCode(pin)
        settingsManager.setLockEnabled(true)
    }

    fun disablePinProtection() {
        settingsManager.setPinCode(null)
        settingsManager.setLockEnabled(false)
        _isAppUnlocked.value = true
    }

    // Search and Filter updates
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFolderFilter(folder: String) {
        _selectedFolder.value = folder
    }

    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
    }

    // Notes CRUD Actions
    fun createOrUpdateNoteInDb(id: Int, title: String, content: String, folder: String, color: String, tags: String, isPinned: Boolean = false, isArchived: Boolean = false) {
        viewModelScope.launch {
            val isNew = id == 0
            val note = Note(
                id = id,
                title = title.ifBlank { "Untitled Note" },
                content = content,
                folder = folder,
                color = color,
                tags = tags,
                isPinned = isPinned,
                isArchived = isArchived,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertNote(note)
            triggerCloudSync()
        }
    }

    fun deleteNoteSilently(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            triggerCloudSync()
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleArchiveNote(note: Note) {
        viewModelScope.launch {
            val nextArchived = !note.isArchived
            repository.insertNote(note.copy(
                isArchived = nextArchived,
                isPinned = if (nextArchived) false else note.isPinned,
                updatedAt = System.currentTimeMillis()
            ))
            triggerCloudSync()
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            val duplicate = note.copy(
                id = 0,
                title = "${note.title} (Copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isPinned = false,
                isSynced = false
            )
            repository.insertNote(duplicate)
            triggerCloudSync()
        }
    }

    // Editor Session state manager (Undo, Redo, Char Count)
    fun setEditorFields(note: Note?) {
        if (note != null) {
            _editorTitle.value = note.title
            _editorContent.value = note.content
            _editorColor.value = note.color
            _editorFolder.value = note.folder
            _editorTags.value = note.tags
        } else {
            _editorTitle.value = ""
            _editorContent.value = ""
            _editorColor.value = "#1E293B" // Default Slate
            _editorFolder.value = "All"
            _editorTags.value = ""
        }
        undoStack.clear()
        redoStack.clear()
    }

    fun onContentChanged(newContent: String) {
        if (newContent != _editorContent.value) {
            undoStack.add(_editorContent.value)
            _editorContent.value = newContent
            redoStack.clear()
        }
    }

    fun onTitleChanged(newTitle: String) {
        _editorTitle.value = newTitle
    }

    fun onColorChanged(newColor: String) {
        _editorColor.value = newColor
    }

    fun onFolderChanged(newFolder: String) {
        _editorFolder.value = newFolder
    }

    fun onTagsChanged(newTags: String) {
        _editorTags.value = newTags
    }

    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val currentText = _editorContent.value
            redoStack.add(currentText)
            _editorContent.value = undoStack.removeAt(undoStack.size - 1)
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val currentText = _editorContent.value
            undoStack.add(currentText)
            _editorContent.value = redoStack.removeAt(redoStack.size - 1)
        }
    }

    // Cloud Sync triggers (Simulates official cloud check)
    fun triggerCloudSync() {
        if (settingsManager.getUserEmail() == null) {
            _syncingState.value = SyncState.Idle
            return
        }
        viewModelScope.launch {
            _syncingState.value = SyncState.Syncing
            // Simulate net check and synchronization with Google Drive
            delay(1500)
            settingsManager.setLastSyncTime(System.currentTimeMillis())
            _syncingState.value = SyncState.Success
            delay(2000)
            _syncingState.value = SyncState.Idle
        }
    }

    // Neural summarize note function
    fun summarizeNoteWithAi(noteContent: String, title: String, onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.generateAiNoteSummary(noteContent, title)
            onCompleted(res)
        }
    }

    // Exports
    fun exportNote(note: Note, type: String, context: Context): File? {
        return try {
            when (type.lowercase()) {
                "markdown" -> repository.exportNoteAsMarkdown(note, context)
                "html" -> repository.exportNoteAsHtml(note, context)
                else -> {
                    // TXT
                    val file = File(context.cacheDir, "${note.title.replace("\\s+".toRegex(), "_")}.txt")
                    file.writeText("${note.title}\n\n${note.content}")
                    file
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val msg: String) : SyncState()
}
