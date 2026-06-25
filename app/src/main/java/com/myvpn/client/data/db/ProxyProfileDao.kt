package com.myvpn.client.data.db

import androidx.room.*
import com.myvpn.client.data.model.ProxyProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyProfileDao {
    @Query("SELECT * FROM proxy_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<ProxyProfile>>

    @Query("SELECT * FROM proxy_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ProxyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProxyProfile): Long

    @Update
    suspend fun updateProfile(profile: ProxyProfile)

    @Delete
    suspend fun deleteProfile(profile: ProxyProfile)

    @Query("UPDATE proxy_profiles SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
}
