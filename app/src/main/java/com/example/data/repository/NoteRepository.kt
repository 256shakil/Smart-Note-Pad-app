package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.Note
import com.example.data.network.GeminiService
import kotlinx.coroutines.flow.Flow
import java.io.File
import android.content.Context

class NoteRepository(private val noteDao: NoteDao) {

    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotesFlow()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotesFlow()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotesFlow()
    val folders: Flow<List<String>> = noteDao.getFoldersFlow()

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        noteDao.deleteNoteById(id)
    }

    suspend fun generateAiNoteSummary(noteContent: String, title: String): String {
        return GeminiService.generateSummary(noteContent, title)
    }

    // Export utilities
    fun exportNoteAsMarkdown(note: Note, context: Context): File {
        val fileName = "${note.title.replace("\\s+".toRegex(), "_")}.md"
        val file = File(context.cacheDir, fileName)
        val markdownContent = """
            # ${note.title}
            
            *Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(note.createdAt)}*
            *Folder: ${note.folder}*
            *Tags: ${note.tags}*
            
            ---
            
            ${note.content}
        """.trimIndent()
        file.writeText(markdownContent)
        return file
    }

    fun exportNoteAsHtml(note: Note, context: Context): File {
        val fileName = "${note.title.replace("\\s+".toRegex(), "_")}.html"
        val file = File(context.cacheDir, fileName)
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; line-height: 1.6; padding: 20px; color: #333; background: #fff; }
                    h1 { border-bottom: 2px solid #eaecef; padding-bottom: 10px; color: #111; }
                    .meta { color: #666; font-size: 0.9em; margin-bottom: 20px; }
                    blockquote { border-left: 4px solid #dfe2e5; color: #6a737d; padding-left: 15px; margin: 0; }
                    code { background-color: rgba(27,31,35,.05); border-radius: 3px; font-family: monospace; padding: .2em .4em; }
                    pre { background-color: #f6f8fa; padding: 16px; border-radius: 6px; overflow: auto; }
                </style>
                <title>${note.title}</title>
            </head>
            <body>
                <h1>${note.title}</h1>
                <div class="meta">
                    <p><b>Created:</b> ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(note.createdAt)}</p>
                    <p><b>Folder:</b> ${note.folder} | <b>Tags:</b> ${note.tags}</p>
                </div>
                <hr/>
                <div>
                     ${note.content.replace("\n", "<br/>")}
                </div>
            </body>
            </html>
        """.trimIndent()
        file.writeText(htmlContent)
        return file
    }
}
