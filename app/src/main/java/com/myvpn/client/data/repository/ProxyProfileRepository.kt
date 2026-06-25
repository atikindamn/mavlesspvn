package com.myvpn.client.data.repository

import com.myvpn.client.data.db.ProxyProfileDao
import com.myvpn.client.data.model.ProxyProfile
import kotlinx.coroutines.flow.Flow

class ProxyProfileRepository(private val dao: ProxyProfileDao) {

    val allProfiles: Flow<List<ProxyProfile>> = dao.getAllProfiles()

    suspend fun getProfileById(id: Long): ProxyProfile? = dao.getProfileById(id)

    suspend fun saveProfile(profile: ProxyProfile): Long {
        return if (profile.id == 0L) {
            dao.insertProfile(profile)
        } else {
            dao.updateProfile(profile)
            profile.id
        }
    }

    suspend fun deleteProfile(profile: ProxyProfile) = dao.deleteProfile(profile)

    suspend fun updateLastConnected(profileId: Long) {
        dao.updateLastConnected(profileId, System.currentTimeMillis())
    }
}
