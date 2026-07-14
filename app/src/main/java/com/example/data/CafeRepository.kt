package com.example.data

import kotlinx.coroutines.flow.Flow

class CafeRepository(private val cafeDao: CafeDao) {
    val allCafes: Flow<List<CafeEntity>> = cafeDao.getAllCafes()

    fun getCafeById(id: Int): Flow<CafeEntity?> = cafeDao.getCafeById(id)

    suspend fun insertCafe(cafe: CafeEntity): Long {
        return cafeDao.insertCafe(cafe)
    }

    suspend fun deleteCafe(cafe: CafeEntity) {
        cafeDao.deleteCafe(cafe)
    }
}
