package com.a2t.myapplication.mediafile.ui.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.R
import com.a2t.myapplication.mediafile.domaim.model.MediaItem

class MediaFileAdapter(
    private val mfac: MediaFileAdapterCallback,
) : RecyclerView.Adapter<MediaFileViewHolder>() {
    val itemList = ArrayList<MediaItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaFileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media_file, parent, false)
        return MediaFileViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaFileViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
        holder.observerItem = Observer { currentHolderItem ->
            holder.selectHolder(currentHolderItem == item)
        }
        holder.observerItem?.let { observer ->
            mfac.getVM().currentHolderItemLiveData.observeForever(observer)
        }

        holder.container.setOnClickListener {
            mfac.getVM().currentHolderItemLiveData.postValue(item)
        }
    }

    override fun onViewRecycled(holder: MediaFileViewHolder) {
        super.onViewRecycled(holder)
        holder.observerItem?.let { observer ->
            holder.selectHolder(false)
            mfac.getVM().currentHolderItemLiveData.removeObserver(observer)
            holder.observerItem = null
        }
    }

    override fun getItemCount() = itemList.size
}