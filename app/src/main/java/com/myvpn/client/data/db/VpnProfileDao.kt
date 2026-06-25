package com.myvpn.client.data.db

import androidx.room.*
import com.myvpn.client.data.model.VpnProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnProfileDao {
    @Query("SELECT * FROM vpn_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<VpnProfile>>

    @Query("SELECT * FROM vpn_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): VpnProfile?

    @Query("SELECT * FROM vpn_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): VpnProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VpnProfile): Long

    @Update
    suspend fun updateProfile(profile: VpnProfile)

    @Delete
    suspend fun deleteProfile(profile: VpnProfile)

    @Query("UPDATE vpn_profiles SET isDefault = 0")
    suspend fun clearDefaultProfile()

    @Query("UPDATE vpn_profiles SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
}
