package com.example.tickit

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// DAO for FavoriteEvent - provides methods to read all favorites, insert, and delete by entity or URL
@Dao
interface FavoriteDao {

    // Returns a LiveData stream of all FavoriteEvent entries in the DB
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): LiveData<List<FavoriteEvent>>

    //  Inserts a FavoriteEvent, replacing any existing entry with the same primary key (URL)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEvent)

    // Deletes the given FavoriteEvent entity (Currently unused)
    @Delete
    suspend fun delete(favorite: FavoriteEvent)

    //Deletes the favorite record matching the given URL
    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}