package com.iptv.player.data.repository

import com.iptv.player.core.util.StreamType
import com.iptv.player.data.local.dao.ResumeDao
import com.iptv.player.data.local.entity.ResumeEntity
import com.iptv.player.domain.repository.ResumeRepository
import javax.inject.Inject

class ResumeRepositoryImpl @Inject constructor(
    private val resumeDao: ResumeDao,
) : ResumeRepository {

    override suspend fun getPosition(accountId: Long, type: StreamType, itemId: Int): Long =
        resumeDao.get(accountId, type, itemId)?.positionMs ?: 0L

    override suspend fun save(
        accountId: Long,
        type: StreamType,
        itemId: Int,
        positionMs: Long,
        durationMs: Long,
    ) {
        val nearEnd = durationMs > 0 && positionMs >= durationMs - END_THRESHOLD_MS
        if (positionMs < MIN_RESUME_MS || nearEnd) {
            resumeDao.delete(accountId, type, itemId)
            return
        }
        resumeDao.upsert(
            ResumeEntity(
                accountId = accountId,
                type = type,
                itemId = itemId,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    private companion object {
        const val MIN_RESUME_MS = 10_000L
        const val END_THRESHOLD_MS = 30_000L
    }
}
