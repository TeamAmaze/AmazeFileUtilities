package com.amaze.fileutilities.utilis

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R

class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    // each data item is just a string in this case
    private val txtTitle: TextView = view.findViewById(R.id.header_title)

    fun setText(headerText: String) {
        txtTitle.text = headerText
    }
}
