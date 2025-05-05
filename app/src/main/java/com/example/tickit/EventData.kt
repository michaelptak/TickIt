package com.example.tickit

import com.google.gson.annotations.SerializedName

data class EventData(
    @SerializedName("_embedded")
    val embedded: Embedded?
)

data class Embedded(
    val events: List<Event>
)

data class Event(
    val name: String?,
    val images: List<Image>,
    val dates: Dates,
    val url: String?,
    @SerializedName("priceRanges")
    val priceRanges: List<PriceRange>,
    @SerializedName("_embedded")
    val embedded: EventEmbedded
)

data class Image(
    val url: String,
    val width: Int,
    val height: Int
)

data class Dates(
    val start: Start
)

data class Start(
    val localDate: String,
    val localTime: String
)

data class EventEmbedded(
    val venues: List<Venue>
)

data class Venue(
    val name: String,
    val city: City,
    val state: State,
    val address: Address,
    @SerializedName("location")
    val location: Location
)

data class Location(
    val latitude: String,
    val longitude: String
)

data class City(
    val name: String
)

data class State(
    val name: String
)

data class Address(
    @SerializedName("line1")
    val line1: String       // street address
)

data class PriceRange(
    val min: Double,
    val max: Double
)
