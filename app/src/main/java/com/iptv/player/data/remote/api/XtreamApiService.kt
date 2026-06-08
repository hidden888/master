package com.iptv.player.data.remote.api

import com.iptv.player.data.remote.dto.AuthResponseDto
import com.iptv.player.data.remote.dto.CategoryDto
import com.iptv.player.data.remote.dto.LiveStreamDto
import com.iptv.player.data.remote.dto.SeriesDto
import com.iptv.player.data.remote.dto.ShortEpgResponseDto
import com.iptv.player.data.remote.dto.VodStreamDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Xtream-Codes player API. The base URL is per-account, so each endpoint takes the full
 * player_api.php [Url] plus the credentials as query parameters.
 */
interface XtreamApiService {

    @GET
    suspend fun authenticate(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
    ): Response<AuthResponseDto>

    @GET
    suspend fun getLiveCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
    ): Response<List<CategoryDto>>

    @GET
    suspend fun getVodCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories",
    ): Response<List<CategoryDto>>

    @GET
    suspend fun getSeriesCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories",
    ): Response<List<CategoryDto>>

    @GET
    suspend fun getLiveStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
        @Query("action") action: String = "get_live_streams",
    ): Response<List<LiveStreamDto>>

    @GET
    suspend fun getVodStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
        @Query("action") action: String = "get_vod_streams",
    ): Response<List<VodStreamDto>>

    @GET
    suspend fun getSeries(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
        @Query("action") action: String = "get_series",
    ): Response<List<SeriesDto>>

    @GET
    suspend fun getShortEpg(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("stream_id") streamId: Int,
        @Query("limit") limit: Int = 10,
        @Query("action") action: String = "get_short_epg",
    ): Response<ShortEpgResponseDto>

    /** Full XMLTV EPG dump (can be several MB); streamed and parsed manually. */
    @Streaming
    @GET
    suspend fun getXmltv(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
    ): Response<ResponseBody>
}
