package com.example.tickit

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

class EventsAdapter(private val events: List<Event>) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    private val favoritedUrls = mutableSetOf<String>()

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageEvent: ImageView = itemView.findViewById<ImageView>(R.id.imageEvent)
        val textEventName = itemView.findViewById<TextView>(R.id.textEventName)
        val textEventDateTime = itemView.findViewById<TextView>(R.id.textEventDateTime)
        val textVenueName = itemView.findViewById<TextView>(R.id.textVenueName)
        val textVenueCityState = itemView.findViewById<TextView>(R.id.textVenueCityState)
        val textVenueAddress = itemView.findViewById<TextView>(R.id.textVenueAddress)
        val textPriceRange = itemView.findViewById<TextView>(R.id.textPriceRange)
        val buttonTicketLink = itemView.findViewById<Button>(R.id.buttonTicketLink)
        val openMapButton = itemView.findViewById<Button>(R.id.openMapButton)
        val favoriteButton = itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.favoriteButton)

        init {
            buttonTicketLink.setOnClickListener {
                // adapterPosition gives current bound item
                val event = events[adapterPosition]
                event.url?.let { link ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    itemView.context.startActivity(intent)
                }
            }

            // Favorite button
            favoriteButton.setOnClickListener {
                val event = events[adapterPosition]
                val url = event.url ?: return@setOnClickListener

                val context = itemView.context
                val db = AppDatabase.getDatabase(context)
                val dao = db.favoriteDao()

                val isNowFavorite: Boolean

                if (favoritedUrls.contains(url)) {
                    // Unfavorite
                    isNowFavorite = false
                    favoritedUrls.remove(url)
                    CoroutineScope(Dispatchers.IO).launch {
                        dao.deleteByUrl(url)
                    }
                } else {
                    // Favorite
                    isNowFavorite = true
                    favoritedUrls.add(url)

                    val json = Gson().toJson(event)
                    val favorite = FavoriteEvent(url, json)

                    CoroutineScope(Dispatchers.IO).launch {
                        dao.insert(favorite)
                    }
                }

                // Update UI
                favoriteButton.isSelected = isNowFavorite
                favoriteButton.text = if (isNowFavorite) "Favorited" else "Favorite"

                val message = if (favoritedUrls.contains(url)) {
                    "Added to favorites"
                } else {
                    "Removed from favorites"
                }

                Snackbar.make(itemView, message, Snackbar.LENGTH_SHORT)
                    .setAnchorView(itemView.rootView.findViewById(R.id.bottom_nav))
                    .show()
            }

            openMapButton.setOnClickListener {
                val venue = events[adapterPosition].embedded.venues.first()
                val lat = venue.location.latitude.toDouble()
                val lng = venue.location.longitude.toDouble()
                val name = venue.name

                //Save into shared view model
                val vm = ViewModelProvider(itemView.context as FragmentActivity)
                    .get(EventsViewModel::class.java)
                vm.selectedLat = lat
                vm.selectedLng = lng
                vm.selectedVenueName = name

                // build a small Bundle of args to pass the venue coordinates
                val args = bundleOf(
                    "EXTRA_LAT" to lat,
                    "EXTRA_LNG" to lng,
                    "EXTRA_VENUE_NAME" to name
                )

                // use NavController to go to the MapsFragment
                // Used to fix back-stack issues
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.mobile_navigation, false)
                    .build()

                itemView.findNavController()
                    .navigate(R.id.mapsFragment, args, navOptions)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_layout, parent, false)
        return EventViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        // Favorites
        holder.favoriteButton.isChecked = favoritedUrls.contains(event.url)
        holder.favoriteButton.text =
            if (holder.favoriteButton.isChecked) "Favorited" else "Favorite"

        // Event Name
        holder.textEventName.text = event.name ?: "No Title"

        // Event Image
        val bestUrl = event.images
            ?.maxByOrNull { it.width * it.height }
            ?.url
        Glide.with(holder.imageEvent.context)
            .load(bestUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .centerCrop()
            .into(holder.imageEvent)

        // Event start date and time
        val date = event.dates?.start?.localDate
        val time = event.dates?.start?.localTime

        if (!date.isNullOrEmpty()) {
            try {
                val formattedDate = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                val formattedTime = if (!time.isNullOrEmpty()) {
                    LocalTime.parse(time).format(DateTimeFormatter.ofPattern("h:mm a"))
                } else {
                    "TBD"
                }
                holder.textEventDateTime.text = "$formattedDate @ $formattedTime"
            } catch (e: Exception) {
                // Fallback in case of parsing error
                holder.textEventDateTime.text = "Date/Time Unavailable"
            }
        } else {
            holder.textEventDateTime.text = "Date/Time TBD"
        }

        // Venue inf
        val venue = event.embedded.venues.first()
        holder.textVenueName.text = venue.name
        holder.textVenueCityState.text = "${venue.city.name}, ${venue.state.name}"
        holder.textVenueAddress.text = venue.address.line1

        // Price range
        val pr = event.priceRanges?.firstOrNull()
        if (pr != null) {
            holder.textPriceRange.visibility = View.VISIBLE
            holder.textPriceRange.text = "$${pr.min.toInt()}  -  $${pr.max.toInt()}"
        } else {
            holder.textPriceRange.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return events.size
    }

    // Refreshes which URLs are currently favorite
    fun setFavoritedUrls(urls: Set<String>) {
        favoritedUrls.clear()
        favoritedUrls.addAll(urls)
        notifyDataSetChanged()  // re-bind all to update button states
    }
}
