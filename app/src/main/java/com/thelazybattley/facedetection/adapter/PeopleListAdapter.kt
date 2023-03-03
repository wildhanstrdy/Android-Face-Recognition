package com.thelazybattley.facedetection.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.thelazybattley.facedetection.classifier.Person


class PeopleListAdapter: ListAdapter<Person, VH>(PeopleDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1,parent,false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}

class PeopleDiff(): DiffUtil.ItemCallback<Person>() {
    override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean {
        return oldItem == newItem
    }

}

class VH(itemView: View): ViewHolder(itemView) {
    fun bind(person: Person){
        itemView.findViewById<TextView>(android.R.id.text1).text = person.name
    }
}
