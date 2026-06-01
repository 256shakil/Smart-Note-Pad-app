package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val folder: String = "All",
    val color: String = "#2D3748", // Cool digital slate as default
    val tags: String = "",       // Comma separated tags: e.g., "work,ideas"
    val isSynced: Boolean = false,
    val reminderTime: Long? = null
)
