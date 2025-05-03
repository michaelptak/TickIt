package com.example.tickit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EventsViewModel: ViewModel() {
    // 1. Livedata to hold the list of events
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    //2. Build retrofit instance inside the ViewModel
    private val ticketMasterAPI = Retrofit.Builder()
        .baseUrl("https://app.ticketmaster.com/discovery/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TicketMasterService::class.java)

    fun searchEvents(apiKey: String, category: String, city: String) {
        ticketMasterAPI.searchEvents(apiKey, category, city)
            .enqueue(object : Callback<EventData> {
                override fun onResponse(call: Call<EventData>, response: Response<EventData>) {
                    val list = response.body()?.embedded?.events.orEmpty()
                    _events.value = list
                }
                override fun onFailure(call: Call<EventData>, t: Throwable) {
                    _events.value = emptyList()
                }
            })
    }
}