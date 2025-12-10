package com.codepath.campgrounds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class CampgroundsFragment : Fragment() {

    private val campgrounds = mutableListOf<Campground>()
    private lateinit var campgroundsRecyclerView: RecyclerView
    private lateinit var campgroundAdapter: CampgroundAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_campgrounds, container, false)

        val layoutManager = LinearLayoutManager(context)
        campgroundsRecyclerView = view.findViewById(R.id.campgrounds)
        campgroundsRecyclerView.layoutManager = layoutManager
        campgroundsRecyclerView.setHasFixedSize(true)
        campgroundAdapter = CampgroundAdapter(view.context, campgrounds)
        campgroundsRecyclerView.adapter = campgroundAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            (activity?.application as? CampgroundApplication)?.db?.campgroundDao()?.getAll()?.collect {
                it.map { entity ->
                    Campground(
                        name = entity.name,
                        description = entity.description,
                        latLong = entity.latLong,
                        images = listOf(CampgroundImage(entity.imageUrl, ""))
                    )
                }.also {
                    campgrounds.clear()
                    campgrounds.addAll(it)
                    campgroundAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    companion object {
        fun newInstance(): CampgroundsFragment {
            return CampgroundsFragment()
        }
    }
}
