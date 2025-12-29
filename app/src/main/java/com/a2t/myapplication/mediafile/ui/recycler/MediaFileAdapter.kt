package com.a2t.myapplication.mediafile.ui.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.R
import com.a2t.myapplication.mediafile.domaim.model.MediaItem

class MediaFileAdapter(
    private val mfac: MediaFileAdapterCallback,
) : RecyclerView.Adapter<MediaFileViewHolder>() {
    val itemList = ArrayList<MediaItem>()
    var currentHolderPositionLiveData = MutableLiveData(-1)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaFileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media_file, parent, false)
        return MediaFileViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaFileViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
        holder.observerPosition = Observer { currentHolderPosition ->
            holder.selectHolder(currentHolderPosition == position)
        }
        currentHolderPositionLiveData.observeForever(holder.observerPosition!!)

        holder.container.setOnClickListener {
            currentHolderPositionLiveData.postValue(position)
        }
    }

    override fun onViewRecycled(holder: MediaFileViewHolder) {
        super.onViewRecycled(holder)
        holder.observerPosition?.let { observer ->
            holder.selectHolder(false)
            currentHolderPositionLiveData.removeObserver(observer)
            holder.observerPosition = null
        }
    }

    override fun getItemCount() = itemList.size
}