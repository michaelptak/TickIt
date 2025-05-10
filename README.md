# TickIt

An Android app that lets you search Ticketmaster events by category and city, view event details, save favorites, and locate venues on a map.

---

## Features
- **Event Search**  
  - Search by category (Music, Sports, Theater, etc.) and city.  
  - Results displayed in a `RecyclerView` with custom `CardView` layouts.  
  - Persistent “last search” using SharedPreferences.

- **MVVM Architecture**  
  - `EventsViewModel` handles network calls (Retrofit + Gson) and exposes LiveData.  
  - Fragments observe the same ViewModel to share state (search results, selected venue).

- **Favorites**  
  - Tap “Favorite” to save an event locally (Room database).  
  - Favorites tab shows all saved events.  
  - Toggle off to remove; database changes propagate via LiveData.

- **Map Integration**  
  - “Find Venue” button opens a Google Map fragment.  
  - Marker placed & info window shown immediately.  
  - Coordinates passed via Navigation arguments or shared ViewModel.

---

## 🔧 Getting Started

1. **Clone the repo**  
   ```bash
   git clone https://github.com/yourusername/tickit.git
   cd TickIt
   ```
2. **Add your Ticketmaster API key**
    In `MainActivity.kt`, repalce:
    ```kotlin
    const val tmApiKey = ""
    ```
    With your own key:
    ```kotlin 
    const val tmApiKey = "YOUR_API_KEY_HERE"
    ```

3. **Add Google Maps API Key** 
    In `local.properties`, set the `MAPS_API_KEY` variable:
    ```bash
    MAPS_API_KEY=YOUR_API_KEY_HERE
    ```
