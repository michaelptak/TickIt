package com.example.tickit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TicketMasterService {

    // base url: https://app.ticketmaster.com/discovery/v2/events.json

    @GET("events.json")
    fun searchEvents(
        @Query("apikey") apiKey: String,
        @Query("keyword") keyword: String,
        @Query("city") city: String,
        @Query("sort") sort: String = "date,asc"
    ): Call<EventData>
}