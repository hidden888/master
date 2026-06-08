package com.iptv.player.data.repository

import com.iptv.player.core.util.PinHasher
import com.iptv.player.data.local.dao.ProfileDao
import com.iptv.player.data.local.entity.ProfileEntity
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.domain.model.Profile
import com.iptv.player.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val pinHasher: PinHasher,
) : ProfileRepository {

    override fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProfile(id: Long): Profile? = profileDao.getById(id)?.toDomain()

    override suspend fun hasProfiles(): Boolean = profileDao.count() > 0

    override suspend fun ensureDefaultProfile(): Long {
        profileDao.firstId()?.let { return it }
        return profileDao.upsert(
            ProfileEntity(name = "Standard", avatarColor = DEFAULT_COLORS.first()),
        )
    }

    override suspend fun createProfile(
        name: String,
        avatarColor: Long,
        isKids: Boolean,
        pin: String?,
    ): Long = profileDao.upsert(
        ProfileEntity(
            name = name.ifBlank { "Profil" },
            avatarColor = avatarColor,
            isKids = isKids,
            pinHash = pin?.takeIf { it.isNotBlank() }?.let { pinHasher.hash(it) },
        ),
    )

    override suspend fun updateProfile(
        id: Long,
        name: String,
        avatarColor: Long,
        isKids: Boolean,
        pin: String?,
    ) {
        val existing = profileDao.getById(id) ?: return
        profileDao.upsert(
            existing.copy(
                name = name.ifBlank { existing.name },
                avatarColor = avatarColor,
                isKids = isKids,
                pinHash = pin?.takeIf { it.isNotBlank() }?.let { pinHasher.hash(it) },
            ),
        )
    }

    override suspend fun deleteProfile(id: Long) = profileDao.delete(id)

    override suspend fun verifyPin(id: Long, pin: String): Boolean {
        val hash = profileDao.getById(id)?.pinHash ?: return true
        return pinHasher.verify(pin, hash)
    }

    private companion object {
        val DEFAULT_COLORS = listOf(0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B, 0xFFEF4444, 0xFFA855F7)
    }
}
