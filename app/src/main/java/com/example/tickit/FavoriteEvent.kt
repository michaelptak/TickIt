package com.example.tickit

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents a favorited event stored in the Room database.
// The URL serves as the unique primary key, and the full Event object is serialized into JSON
@Entity(tableName = "favorites")
data class FavoriteEvent(
    @PrimaryKey val url: String,
    val json: String
)
