package com.example.tickit

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.*
import android.view.*
import android.widget.*
import androidx.navigation.*
import java.time.*
import java.time.format.*

// RecyclerView Adapter for displaying a list of Ticketmaster events,
// handling click actions (open link, favorite/unfavorite, show on map),
// and reflecting favorite state

class EventsAdapter(private val events: List<Event>) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    // Tracks which event URLs are currently favorited
    private val favoritedUrls = mutableSetOf<String>()

     //ViewHolder caches all view references for a single event row,
     //sets up one-time click listeners for link, favorite, and map actions.
    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageEvent: ImageView = itemView.findViewById(R.id.imageEvent)
        val textEventName: TextView = itemView.findViewById(R.id.textEventName)
        val textEventDateTime: TextView = itemView.findViewById(R.id.textEventDateTime)
        val textVenueName: TextView = itemView.findViewById(R.id.textVenueName)
        val textVenueCityState: TextView = itemView.findViewById(R.id.textVenueCityState)
        val textVenueAddress: TextView = itemView.findViewById(R.id.textVenueAddress)
        val textPriceRange: TextView = itemView.findViewById(R.id.textPriceRange)
        private val buttonTicketLink: Button = itemView.findViewById(R.id.buttonTicketLink)
        private val openMapButton: Button = itemView.findViewById(R.id.openMapButton)
        val favoriteButton: MaterialButton = itemView.findViewById(R.id.favoriteButton)

        init {
            // Open Ticketmaster link in external browser
            buttonTicketLink.setOnClickListener {
                val event = events[adapterPosition]
                event.url?.let { link ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    itemView.context.startActivity(intent)
                }
            }

            // Toggle favorite state and update Room DB asynchronously
            favoriteButton.setOnClickListener {
                val event = events[adapterPosition]
                val url = event.url ?: return@setOnClickListener

                val context = itemView.context
                val dao = AppDatabase.getDatabase(context).favoriteDao()
                val nowFavorited: Boolean

                if (favoritedUrls.contains(url)) {
                    nowFavorited = false
                    favoritedUrls.remove(url)
                    // Delete in the background
                    CoroutineScope(Dispatchers.IO).launch {
                        dao.deleteByUrl(url)
                    }
                } else {
                    nowFavorited = true
                    favoritedUrls.add(url)
                    val json = Gson().toJson(event)
                    val favorite = FavoriteEvent(url, json)
                    // Insert in the background
                    CoroutineScope(Dispatchers.IO).launch {
                        dao.insert(favorite)
                    }
                }

                // Update UI Button
                favoriteButton.isChecked = nowFavorited
                favoriteButton.text = if (nowFavorited) "Favorited" else "Favorite"

                // Show confirmation Snackbar anchored to bottom nav
                Snackbar.make(itemView,
                    if (nowFavorited) "Added to favorites" else "Removed from favorites",
                    Snackbar.LENGTH_SHORT)
                    .setAnchorView(itemView.rootView.findViewById(R.id.bottom_nav))
                    .show()
            }

            // Navigate to MapsFragment, passing coordinates via shared ViewModel and NavController
            openMapButton.setOnClickListener {
                val venue = events[adapterPosition].embedded.venues.first()
                val lat = venue.location.latitude.toDouble()
                val lng = venue.location.longitude.toDouble()
                val name = venue.name

                // Store selection in shared ViewModel
                val vm = ViewModelProvider(itemView.context as FragmentActivity)[EventsViewModel::class.java]
                vm.selectedLat = lat
                vm.selectedLng = lng
                vm.selectedVenueName = name

                // Build args and navigate
                val args = bundleOf(
                    "EXTRA_LAT" to lat,
                    "EXTRA_LNG" to lng,
                    "EXTRA_VENUE_NAME" to name
                )
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.mobile_navigation, false)
                    .build()
                itemView.findNavController()
                    .navigate(R.id.mapsFragment, args, navOptions)
            }
        }
    }

    // Inflates the row layout and wraps in an EventViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.event_layout, parent, false)
        return EventViewHolder(view)
    }

    // Binds the data from the events[position] into each view,
    // formats data/time, handles visiblity of optional fields
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        // Reflect current favorite state
        holder.favoriteButton.isChecked = favoritedUrls.contains(event.url)
        holder.favoriteButton.text =
            if (holder.favoriteButton.isChecked) "Favorited" else "Favorite"

        // Event Name
        holder.textEventName.text = event.name ?: "No Title"

        // Load best image via Glide
        val bestUrl = event.images.maxByOrNull { it.width * it.height }?.url
        Glide.with(holder.imageEvent.context)
            .load(bestUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .centerCrop()
            .into(holder.imageEvent)

        // Format date/time or show TBD
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

        // Venue information
        val venue = event.embedded.venues.first()
        holder.textVenueName.text = venue.name
        holder.textVenueCityState.text = "${venue.city.name}, ${venue.state.name}"
        holder.textVenueAddress.text = venue.address.line1

        // Price range (if available)
        val pr = event.priceRanges?.firstOrNull()
        if (pr != null) {
            holder.textPriceRange.visibility = View.VISIBLE
            holder.textPriceRange.text = "$${pr.min.toInt()}  -  $${pr.max.toInt()}"
        } else {
            holder.textPriceRange.visibility = View.GONE
        }
    }

    // Return total number of events in the list (unused)
    override fun getItemCount(): Int {
        return events.size
    }

    // Refreshes which URLs are currently favorite
    fun setFavoritedUrls(urls: Set<String>) {
        favoritedUrls.clear()
        favoritedUrls += urls
        notifyDataSetChanged()
    }
}