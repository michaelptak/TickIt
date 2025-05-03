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
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class EventsAdapter(private val events: List<Event>) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageEvent = itemView.findViewById<ImageView>(R.id.imageEvent)
        val textEventName = itemView.findViewById<TextView>(R.id.textEventName)
        val textEventDateTime = itemView.findViewById<TextView>(R.id.textEventDateTime)
        val textVenueName = itemView.findViewById<TextView>(R.id.textVenueName)
        val textVenueCityState = itemView.findViewById<TextView>(R.id.textVenueCityState)
        val textVenueAddress = itemView.findViewById<TextView>(R.id.textVenueAddress)
        val textPriceRange = itemView.findViewById<TextView>(R.id.textPriceRange)
        val buttonTicketLink = itemView.findViewById<Button>(R.id.buttonTicketLink)

        init {
            buttonTicketLink.setOnClickListener {
                // adapterPosition gives current bound item
                val event = events[adapterPosition]
                event.url?.let { link ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    itemView.context.startActivity(intent)
                }
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
}
