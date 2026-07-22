package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CafeEntity
import com.example.data.CafeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CafeViewModel(private val repository: CafeRepository) : ViewModel() {

    val allCafes: StateFlow<List<CafeEntity>> = repository.allCafes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedCafe = MutableStateFlow<CafeEntity?>(null)
    val selectedCafe: StateFlow<CafeEntity?> = _selectedCafe.asStateFlow()

    private val _isDarkTheme = MutableStateFlow<Boolean?>(null) // null means follow system theme
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(context: Context, value: Boolean?) {
        _isDarkTheme.value = value
        saveThemePreference(context, value)
    }

    fun loadThemePreference(context: Context) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val valueStr = prefs.getString("theme_mode", "auto")
        _isDarkTheme.value = when (valueStr) {
            "light" -> false
            "dark" -> true
            else -> null
        }
    }

    private fun saveThemePreference(context: Context, value: Boolean?) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val valueStr = when (value) {
            false -> "light"
            true -> "dark"
            else -> "auto"
        }
        prefs.edit().putString("theme_mode", valueStr).apply()
    }

    private val _editingCafe = MutableStateFlow<CafeEntity?>(null)
    val editingCafe: StateFlow<CafeEntity?> = _editingCafe.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun selectCafe(cafe: CafeEntity?) {
        _selectedCafe.value = cafe
    }

    fun setEditingCafe(cafe: CafeEntity?) {
        _editingCafe.value = cafe
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun addCafe(
        context: Context,
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        rating: Int,
        coffeeQualityRating: Int,
        atmosphereRating: Int,
        notes: String,
        selectedPhotoUris: List<Uri>,
        mapShareLink: String? = null,
        tags: String? = null,
        facilities: String? = null,
        favoriteDrink: String? = null
    ) {
        viewModelScope.launch {
            try {
                val savedPhotoPaths = mutableListOf<String>()
                
                // Copy selected photos to internal storage to persist them
                selectedPhotoUris.forEach { uri ->
                    val path = saveImageToInternalStorage(context, uri)
                    if (path != null) {
                        savedPhotoPaths.add(path)
                    }
                }

                val primaryPhoto = savedPhotoPaths.firstOrNull()
                val secondaryPhotos = if (savedPhotoPaths.size > 1) {
                    savedPhotoPaths.drop(1).joinToString(";")
                } else null

                val newCafe = CafeEntity(
                    name = name,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    rating = rating,
                    coffeeQualityRating = coffeeQualityRating,
                    atmosphereRating = atmosphereRating,
                    notes = notes,
                    photoUri = primaryPhoto,
                    photoUris = secondaryPhotos,
                    mapShareLink = mapShareLink,
                    tags = tags,
                    facilities = facilities,
                    favoriteDrink = favoriteDrink
                )

                repository.insertCafe(newCafe)
                _saveSuccess.value = true
                Log.d("CafeViewModel", "Successfully saved cafe log: $name")
            } catch (e: Exception) {
                Log.e("CafeViewModel", "Failed to save cafe: ${e.message}", e)
            }
        }
    }

    fun updateCafe(
        context: Context,
        id: Int,
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        rating: Int,
        coffeeQualityRating: Int,
        atmosphereRating: Int,
        notes: String,
        keptPhotoPaths: List<String>,
        newPhotoUris: List<Uri>,
        mapShareLink: String? = null,
        tags: String? = null,
        facilities: String? = null,
        favoriteDrink: String? = null
    ) {
        viewModelScope.launch {
            try {
                val savedPhotoPaths = mutableListOf<String>()
                // Add kept photos
                savedPhotoPaths.addAll(keptPhotoPaths)
                
                // Copy new photos to internal storage to persist them
                newPhotoUris.forEach { uri ->
                    val path = saveImageToInternalStorage(context, uri)
                    if (path != null) {
                        savedPhotoPaths.add(path)
                    }
                }

                val primaryPhoto = savedPhotoPaths.firstOrNull()
                val secondaryPhotos = if (savedPhotoPaths.size > 1) {
                    savedPhotoPaths.drop(1).joinToString(";")
                } else null

                val updatedCafe = CafeEntity(
                    id = id,
                    name = name,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    rating = rating,
                    coffeeQualityRating = coffeeQualityRating,
                    atmosphereRating = atmosphereRating,
                    notes = notes,
                    photoUri = primaryPhoto,
                    photoUris = secondaryPhotos,
                    mapShareLink = mapShareLink,
                    tags = tags,
                    facilities = facilities,
                    favoriteDrink = favoriteDrink
                )

                repository.insertCafe(updatedCafe)
                _saveSuccess.value = true
                Log.d("CafeViewModel", "Successfully updated cafe log: $name")
            } catch (e: Exception) {
                Log.e("CafeViewModel", "Failed to update cafe: ${e.message}", e)
            }
        }
    }

    fun deleteCafe(cafe: CafeEntity) {
        viewModelScope.launch {
            try {
                // Delete associated photo files if stored internally
                cafe.photoUri?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                cafe.photoUris?.split(";")?.forEach { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                
                repository.deleteCafe(cafe)
                if (_selectedCafe.value?.id == cafe.id) {
                    _selectedCafe.value = null
                }
            } catch (e: Exception) {
                Log.e("CafeViewModel", "Failed to delete cafe: ${e.message}", e)
            }
        }
    }

    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            
            // Create a unique filename in internal files dir
            val dir = File(context.filesDir, "cafe_photos")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val filename = "cafe_${UUID.randomUUID()}.jpg"
            val file = File(dir, filename)
            
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("CafeViewModel", "Error copying photo: ${e.message}", e)
            null
        }
    }
}
