package com.example.tickit
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// A reusable fragment to implement the same logic for Events and Favorites fragments.
 // Subclasses must implement subscribeToData() to observe their data source.
abstract class BaseListFragment : Fragment(R.layout.fragment_events_list) {

    protected lateinit var viewModel: EventsViewModel
    private val items = ArrayList<Event>()
    private lateinit var adapter: EventsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Grab the shared ViewModel
        viewModel = ViewModelProvider(requireActivity())[EventsViewModel::class.java]

        // RecyclerView + adapter
        recyclerView = view.findViewById(R.id.recyclerView)
        noResultsTextView = view.findViewById(R.id.noResultsTextView)
        adapter = EventsAdapter(items)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Always observe favorites so buttonS stay in sync
        viewModel.favorites.observe(viewLifecycleOwner) { favs ->
            val urls = favs.mapNotNull { it.url }.toSet()
            adapter.setFavoritedUrls(urls)
        }

        // Let subclass hook up its own data stream
        subscribeToData()
    }

    // Called once in onViewCreated, Subclasses observe their own LiveData sources
    protected abstract fun subscribeToData()

     //Replaces items in the list, notifies the adapter, and toggles the “no results” view.
    protected fun updateList(newItems: List<Event>) {
        items.clear()
        items.addAll(newItems)
        adapter.notifyDataSetChanged()

        if (newItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            noResultsTextView.visibility = View.VISIBLE
        } else {
            noResultsTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
