package com.example.tickit

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEvent(
    @PrimaryKey val url: String,
    val json: String
)
