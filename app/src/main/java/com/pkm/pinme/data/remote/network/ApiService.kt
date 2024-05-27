package com.pkm.pinme.data.remote.network

import com.pkm.pinme.data.remote.response.FilterResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("/filter/{Id}")
    suspend fun getFilter(@Path("Id") filterId: String) : FilterResponse
}
