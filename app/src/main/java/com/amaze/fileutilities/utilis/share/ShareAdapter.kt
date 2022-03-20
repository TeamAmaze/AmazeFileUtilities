/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis.share

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R

class ShareAdapter(
    val context: Context,
    private val intents: List<Intent>,
    private val labels: List<String>,
    private val drawables: List<Drawable>
) : RecyclerView.Adapter<ShareAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.share_adapter_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.run {
            imageView.setImageDrawable(drawables[position])
            textView.visibility = View.VISIBLE
            textView.text = labels[position]
            rootView.setOnClickListener {
                context.startActivity(intents[position])
            }
        }
    }

    class ViewHolder(val rootView: View) : RecyclerView.ViewHolder(
        rootView
    ) {

        @JvmField
        val textView: TextView = rootView.findViewById(R.id.firstline)

        @JvmField
        val imageView: ImageView = rootView.findViewById(R.id.icon)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return intents.size
    }
}
