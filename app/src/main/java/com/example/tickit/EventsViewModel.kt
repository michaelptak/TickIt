package com.example.tickit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Viewmodel for:
// - Exposing LiveData of Ticketmaster search results
// - Persisting and exposing favorite events from Room
// - Remembering the last-selected venue coordinates for map screen

class EventsViewModel(application: Application) : AndroidViewModel(application) {
    // Livedata to hold the list of events
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    // These values persist across configuration changes and are used by the MapsFragment
    var selectedLat: Double? = null
    var selectedLng: Double? = null
    var selectedVenueName: String? = null

    // Build retrofit instance inside the ViewModel
    private val ticketMasterAPI = Retrofit.Builder()
        .baseUrl("https://app.ticketmaster.com/discovery/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TicketMasterService::class.java)

    // Queries the Ticketmaster API with specified apiKey, category, and city.
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

    /// Obtain the DAO once and expose LiveData of all FavoriteEvent enties
    private val dao = AppDatabase
        .getDatabase(application)
        .favoriteDao()
    val favorites: LiveData<List<FavoriteEvent>> = dao.getAllFavorites()

}