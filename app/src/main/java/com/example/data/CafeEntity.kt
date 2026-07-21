package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cafes")
data class CafeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Int, // 1 to 5
    val coffeeQualityRating: Int, // 1 to 5
    val atmosphereRating: Int, // 1 to 5
    val notes: String,
    val photoUri: String?, // Primary photo URI
    val photoUris: String? = null, // Semicolon-separated secondary photo URIs if any
    val mapShareLink: String? = null, // Google Maps share link
    val tags: String? = null, // Semicolon-separated tags (e.g. "work-friendly;cosy;espresso bar")
    val favoriteDrink: String? = null, // Favorite drink name (e.g. "Iced oat latte")
    val timestamp: Long = System.currentTimeMillis()
)
