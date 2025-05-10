package com.example.tickit

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

//private const val TAG = "MainActivity"
const val tmApiKey = "dAJZsZGUAmNjkma5WPyGWu9wKbkTMw6Q"

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: EventsViewModel
    private val ACCESS_LOCATION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bottom Navigation
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

            // Navigate back to events fragment even when pressed from a different one
            bottomNav.selectedItemId = R.id.eventsListFragment

            // Hide the keyboard after the search
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
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

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        val rootView = findViewById<View>(android.R.id.content)

        // Get the last known location (network first)
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
                    .setAction("OK") { /* no option, just dismiss */ }
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