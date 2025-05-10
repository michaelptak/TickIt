package com.example.tickit

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.LocationManager
import android.content.Context
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import android.view.inputmethod.InputMethodManager
import android.view.*
import android.widget.*

// Include Ticketmaster API key here
const val tmApiKey = "dAJZsZGUAmNjkma5WPyGWu9wKbkTMw6Q"

// Hosts the BottomNavigationView, search controls (city/category), and location permission logic.
//Coordinates between UI (fragments) and EventsViewModel for searches, favorites, and maps.
class MainActivity : AppCompatActivity() {

    // ViewModel shared across fragments for search results, favorites, and selected venue
    private lateinit var viewModel: EventsViewModel

    // Arbitrary request code for location permission
    private val ACCESS_LOCATION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bottom Navigation Setup
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[EventsViewModel::class.java]

        // Initialize SharedPreferences and Views
        val sharedPreferences = getSharedPreferences("TicketMasterPrefs", MODE_PRIVATE)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val locationButton = findViewById<Button>(R.id.locationButton)
        val spinner: Spinner = findViewById(R.id.spinner)

        // Category spinner setup
        val categories = listOf(
            "Choose an event category",
            "Music", "Sports", "Theater", "Family",
            "Arts & Theater", "Concerts", "Comedy", "Dance"
        )
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

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

        // Location Button
        locationButton.setOnClickListener {
            getLocationPermission()
        }

        // Search Button
        searchButton.setOnClickListener {
            // Validate category selection
            val selectedCategory = spinner.selectedItem.toString()
            if (selectedCategory == categories[0]) {
                showAlertDialog(
                    "Select a category",
                    "Event category cannot be empty. Please select an event category."
                )
                return@setOnClickListener
            }

            // Validate city input
            val city = searchEditText.text.toString()
            if (city.isEmpty()) {
                showAlertDialog(
                    "Location Missing",
                    "City cannot be empty. Please enter a city."
                )
                return@setOnClickListener
            }

            // Save the last search to SharedPreferences
            sharedPreferences.edit().apply {
                putString("last_city", city)
                putString("last_category", selectedCategory)
                apply()
            }

            // API Call
            viewModel.searchEvents(tmApiKey, selectedCategory, city)

            // Navigate back to events fragment even when pressed from a different one
            bottomNav.selectedItemId = R.id.eventsListFragment

            // Hide the keyboard after the search
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(searchEditText.windowToken, 0)
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

    // Handle the permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ACCESS_LOCATION_CODE) {
            val rootView = findViewById<View>(android.R.id.content)

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onLocationPermissionGranted()
            } else {
                Snackbar.make(rootView, "Location permission denied", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.bottom_nav)
                    .setBackgroundTint(
                        ContextCompat.getColor(this, R.color.brand_secondary)
                    )
                    .setTextColor(
                        ContextCompat.getColor(this, R.color.brand_onSecondary)
                    )
                    .setAction("Retry") {
                        getLocationPermission()
                    }
                    .show()
            }
        }
    }

    // When permission is granted
    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        val rootView = findViewById<View>(android.R.id.content)
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            // Reverse‐geocode to a city name
            val geocoder = Geocoder(this)
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: ""
                // Populate the search field
                findViewById<EditText>(R.id.searchEditText).setText(city)
                Snackbar.make(rootView, "Auto‐filled city: $city", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.bottom_nav)
                    .setAction("OK", null)
                    .show()
            }
        } else {
            Snackbar.make(rootView, "Unable to fetch location", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.bottom_nav)
                .setAction("Retry") { getLocationPermission() }
                .show()
        }
    }
}