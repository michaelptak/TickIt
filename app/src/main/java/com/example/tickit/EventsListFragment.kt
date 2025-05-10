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

class EventsListFragment : Fragment() {

    private lateinit var viewModel: EventsViewModel
    private val events = ArrayList<Event>()
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_events_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // grab the shared ViewModel
        viewModel = ViewModelProvider(requireActivity())[EventsViewModel::class.java]

        // Setup RecyclerView + Adapter
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        adapter = EventsAdapter(events)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Favorites view model
        viewModel.favorites.observe(viewLifecycleOwner) { favEvents ->
            val favUrls = favEvents.map { it.url }.toSet()
            adapter.setFavoritedUrls(favUrls)
        }

        val noResultsTextView = view.findViewById<TextView>(R.id.noResultsTextView)

        viewModel.events.observe(viewLifecycleOwner) { list ->
            events.clear()
            events.addAll(list)
            adapter.notifyDataSetChanged()

            if (list.isEmpty()) {
                recyclerView.visibility = View.GONE
                noResultsTextView.visibility = View.VISIBLE
            } else {
                noResultsTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }
}
