package com.example.tickit

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.LocationManager
import android.content.Context


//private const val TAG = "MainActivity"
const val tmApiKey = ""

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: EventsViewModel
    private val ACCESS_LOCATION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[EventsViewModel::class.java]

        // Initialize SharedPreferences and Views
        val sharedPreferences = getSharedPreferences("TicketMasterPrefs", MODE_PRIVATE)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val noResultsTextView = findViewById<TextView>(R.id.noResultsTextView).apply { visibility = View.GONE }
        val locationButton = findViewById<Button>(R.id.locationButton)

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

        locationButton.setOnClickListener {
            getLocationPermission()
        }

        // RecyclerView
        val recyclerView= findViewById<RecyclerView>(R.id.recyclerView)
        val eventList = ArrayList<Event>()
        val adapter = EventsAdapter(eventList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Observe LiveData from ViewModel
        viewModel.events.observe(this) { list ->
            eventList.clear()
            eventList.addAll(list)
            adapter.notifyDataSetChanged()

            // show/hide “no results”
            if (list.isEmpty()) {
                recyclerView.visibility = View.GONE
                noResultsTextView.visibility = View.VISIBLE
            } else {
                noResultsTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        //handle spinner
        val categoryAdapter = ArrayAdapter(
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
                showAlertDialog(
                    "Select a category",
                    "Event category cannot be empty. Please select an event category."
                )
                return@setOnClickListener
            }

            val city = searchEditText.text.toString()
            if (city.isEmpty()) {
                showAlertDialog(
                    "Location Missing",
                    "City cannot be empty. Please enter a city."
                )
                return@setOnClickListener
            }

            // Save the last search to SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("last_city", city)
            editor.putString("last_category", selectedCategory)
            editor.apply()

            // API Call
            viewModel.searchEvents(tmApiKey, selectedCategory, city)
        }
    }

    // Helper function for Dialogs
    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Helper function for getting location permission
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // show the user rationale and re-prompt
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs access to your location to auto-fill your city for event searches.")
                    .setPositiveButton("OK") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            ACCESS_LOCATION_CODE
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ACCESS_LOCATION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ACCESS_LOCATION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onLocationPermissionGranted()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show()

        // Get the last known location (network first)
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location =
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            // 2) Reverse‐geocode to a city name
            val geocoder = Geocoder(this)
            val addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: ""
                // Populate the search field
                findViewById<EditText>(R.id.searchEditText)
                    .setText(city)
            }
        } else {
            Toast.makeText(
                this,
                "Unable to fetch current location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}