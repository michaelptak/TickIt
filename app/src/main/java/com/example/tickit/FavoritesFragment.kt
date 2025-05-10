package com.example.tickit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson

class FavoritesFragment : Fragment() {

    private lateinit var viewModel: EventsViewModel
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ):  View = inflater.inflate(R.layout.fragment_events_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val noResultsTextView = view.findViewById<TextView>(R.id.noResultsTextView)
        val list = ArrayList<Event>()

        adapter = EventsAdapter(list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[EventsViewModel::class.java]

        viewModel.favorites.observe(viewLifecycleOwner) { favs ->
            val favUrls = favs.map { it.url }.toSet()
            adapter.setFavoritedUrls(favUrls)

            val events = favs.map { Gson().fromJson(it.json, Event::class.java) }
            list.clear()
            list.addAll(events)
            adapter.notifyDataSetChanged()

            if (events.isEmpty()) {
                recyclerView.visibility = View.GONE
                noResultsTextView.visibility = View.VISIBLE
            } else {
                noResultsTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }
}