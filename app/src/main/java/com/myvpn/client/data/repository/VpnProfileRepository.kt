package com.myvpn.client.data.repository

import com.myvpn.client.data.db.VpnProfileDao
import com.myvpn.client.data.model.VpnProfile
import kotlinx.coroutines.flow.Flow

class VpnProfileRepository(private val dao: VpnProfileDao) {

    val allProfiles: Flow<List<VpnProfile>> = dao.getAllProfiles()

    suspend fun getProfileById(id: Long): VpnProfile? = dao.getProfileById(id)

    suspend fun getDefaultProfile(): VpnProfile? = dao.getDefaultProfile()

    suspend fun saveProfile(profile: VpnProfile): Long {
        return if (profile.id == 0L) {
            dao.insertProfile(profile)
        } else {
            dao.updateProfile(profile)
            profile.id
        }
    }

    suspend fun deleteProfile(profile: VpnProfile) = dao.deleteProfile(profile)

    suspend fun setDefaultProfile(profileId: Long) {
        dao.clearDefaultProfile()
        dao.getProfileById(profileId)?.let {
            dao.updateProfile(it.copy(isDefault = true))
        }
    }

    suspend fun updateLastConnected(profileId: Long) {
        dao.updateLastConnected(profileId, System.currentTimeMillis())
    }
}
