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

        // 1) Read the arguments *or* the ViewModel
        val vm = ViewModelProvider(requireActivity())
            .get(EventsViewModel::class.java)

        val lat = arguments?.getDouble("EXTRA_LAT")
            ?: vm.selectedLat
            ?: -34.0
        val lng = arguments?.getDouble("EXTRA_LNG")
            ?: vm.selectedLng
            ?: 151.0
        val title = arguments?.getString("EXTRA_VENUE_NAME")
            ?: vm.selectedVenueName
            ?: "Venue"

        // 2) Drop the marker exactly once
        val pos = LatLng(lat, lng)
        val marker = mMap.addMarker(
            MarkerOptions().position(pos).title(title)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
        marker?.showInfoWindow()
    }
}
