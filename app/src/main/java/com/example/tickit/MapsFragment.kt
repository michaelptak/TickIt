package com.example.tickit

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

// Displays a Google Map centered on the event venue.
// Reads coordinates/title from fragment arguments or falls back to the shared EventsViewModel
class MapsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_maps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFrag = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //  Retrieve coordinates and title from args or fall back to ViewModel defaults (or, arbitrarily to default of Sydney in case nothing has been passed yet)
        val vm = ViewModelProvider(requireActivity())[EventsViewModel::class.java]
        val lat = arguments?.getDouble("EXTRA_LAT") ?: vm.selectedLat ?: -34.0
        val lng = arguments?.getDouble("EXTRA_LNG") ?: vm.selectedLng ?: 151.0
        val title = arguments?.getString("EXTRA_VENUE_NAME")
            ?: vm.selectedVenueName
            ?: "Venue"

        // Add a single marker and move the camera
        val pos = LatLng(lat, lng)
        val marker = mMap.addMarker(
            MarkerOptions().position(pos).title(title)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
        marker?.showInfoWindow()
    }
}
