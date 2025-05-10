package com.example.tickit

import com.google.gson.Gson

class FavoritesFragment : BaseListFragment() {
    override fun subscribeToData() {
        viewModel.favorites.observe(viewLifecycleOwner) { favs ->
            // Map FavoriteEvent JSON back into Event objects
            val events = favs.map { Gson().fromJson(it.json, Event::class.java) }
            updateList(events)
        }
    }
}