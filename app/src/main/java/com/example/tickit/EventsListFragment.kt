package com.example.tickit

class EventsListFragment : BaseListFragment() {
    override fun subscribeToData() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            updateList(events)
        }
    }
}