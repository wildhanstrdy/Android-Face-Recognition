package com.thelazybattley.facedetection.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.thelazybattley.facedetection.R
import com.thelazybattley.facedetection.classifier.People


class PeopleListAdapter: ListAdapter<People, VH>(PeopleDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1,parent,false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}

class PeopleDiff(): DiffUtil.ItemCallback<People>() {
    override fun areItemsTheSame(oldItem: People, newItem: People): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: People, newItem: People): Boolean {
        return oldItem == newItem
    }

}

class VH(itemView: View): ViewHolder(itemView) {
    fun bind(people: People){
        itemView.findViewById<TextView>(android.R.id.text1).text = people.name
    }
}
