
package com.repodoctor.mobile.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface RepoDoctorApi {

    @GET("api/health")
    suspend fun health(): Map<String, String>

    @POST("api/repository/index")
    suspend fun indexRepository(
        @Body request: IndexRequest
    ): IndexResponse

    @POST("api/ask")
    suspend fun ask(
        @Body request: AskRequest
    ): AIResponse

    @POST("api/debug")
    suspend fun debug(
        @Body request: DebugRequest
    ): AIResponse

    @POST("api/tests")
    suspend fun generateTests(
        @Body request: TestRequest
    ): AIResponse

    @GET("api/history")
    suspend fun getHistory(
        @Query("limit") limit: Int = 50
    ): List<HistoryItem>

    @DELETE("api/history/{itemId}")
    suspend fun deleteHistory(
        @Path("itemId") itemId: Int
    ): Map<String, Any>
}
