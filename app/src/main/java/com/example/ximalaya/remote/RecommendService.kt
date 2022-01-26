package com.example.ximalaya.remote

import com.example.ximalaya.model.Album
import com.example.ximalaya.other.Constants.LIKE_KEY
import retrofit2.http.GET
import retrofit2.http.Query

interface RecommendService {
    @GET("/v2/albums/guess_like?$LIKE_KEY")
    suspend fun searchRecommendData(
        @Query("access_token") accessToken: String,
        @Query("sig") sig: String
    ): List<Album>
}