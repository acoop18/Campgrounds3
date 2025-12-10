package com.codepath.campgrounds

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ParksAdapter(private val context: Context, private val parks: List<Park>) :
    RecyclerView.Adapter<ParksAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_park, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val park = parks[position]
        holder.bind(park)
    }

    override fun getItemCount() = parks.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        private val parkImageView: ImageView = itemView.findViewById(R.id.image)

        fun bind(park: Park) {
            nameTextView.text = park.fullName
            descriptionTextView.text = park.description

            Glide.with(context)
                .load(park.imageUrl)
                .into(parkImageView)
        }
    }
}
