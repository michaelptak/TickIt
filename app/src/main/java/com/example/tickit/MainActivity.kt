package com.example.tickit

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "MainActivity"
val tmApiKey = ""

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val sharedPreferences = getSharedPreferences("TicketMasterPrefs", MODE_PRIVATE)

        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val noResultsTextView = findViewById<TextView>(R.id.noResultsTextView).apply { visibility = View.GONE }

        val categories = listOf(
            "Choose an event category",
            "Music",
            "Sports",
            "Theater",
            "Family",
            "Arts & Theater",
            "Concerts",
            "Comedy",
            "Dance"
        )

        // RecyclerView
        val recyclerView= findViewById<RecyclerView>(R.id.recyclerView)
        val eventList = ArrayList<Event>()
        val adapter = EventsAdapter(eventList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        //Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://app.ticketmaster.com/discovery/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val ticketMasterAPI = retrofit.create(TicketMasterService::class.java)

        //handle spinner
        val categoryAdapter = ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_dropdown_item, categories)
        val spinner: Spinner = findViewById(R.id.spinner)
        spinner.adapter = categoryAdapter

        // Load last search using SharedPreferences
        val lastCity = sharedPreferences.getString("last_city", "")
        val lastCategory = sharedPreferences.getString("last_category", "")

        searchEditText.setText(lastCity)

        if (!lastCategory.isNullOrEmpty()) {
            val position = categories.indexOf(lastCategory)
            if (position >= 0) {
                spinner.setSelection(position)
            }
        }

        // Handle search button
        searchButton.setOnClickListener {
            val selectedCategory = spinner.selectedItem.toString()
            if (selectedCategory == categories[0]) {
                AlertDialog.Builder(this)
                    .setTitle("Select a category")
                    .setMessage("Event category cannot be empty. Please select an event category.")
                    .setPositiveButton("OK", null)
                    .show()
            }

            val city = searchEditText.text.toString()
            if (city.isEmpty() || selectedCategory == "Choose an event category") {
                AlertDialog.Builder(this)
                    .setTitle("Location Missing")
                    .setMessage("City cannot be empty. Please enter a city.")
                    .setPositiveButton("OK", null)
                    .show()
            }

            // Save the last search to SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("last_city", city)
            editor.putString("last_category", selectedCategory)
            editor.apply()

            // API Call
            ticketMasterAPI.searchEvents(tmApiKey, selectedCategory, city).enqueue(object : Callback<EventData> {
                override fun onResponse(call: Call<EventData>, response: Response<EventData>) {
                    // logging purposes
                    Log.d(TAG, "onResponse: $response")
                    val body = response.body()
                    if (body == null) {
                        Log.w(TAG, "Valid response was not received")
                        return
                    }
                    //populate recyclerview
                    eventList.clear()
                    val events = body.embedded?.events.orEmpty()
                    if (events.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        noResultsTextView.visibility = View.VISIBLE
                    } else {
                        noResultsTextView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        eventList.addAll(events)
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<EventData>, t: Throwable) {
                    Log.d(TAG, "onFailure : $t")
                }
            })


        }

    }
}