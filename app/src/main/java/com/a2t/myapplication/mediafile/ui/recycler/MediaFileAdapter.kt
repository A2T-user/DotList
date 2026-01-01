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
        holder.observerUri = Observer { currentHolderUri ->
            holder.selectHolder(currentHolderUri == item.uri)
        }
        holder.observerUri?.let { observer ->
            mfac.getVM().currentHolderUriLiveData.observeForever(observer)
        }

        holder.container.setOnClickListener {
            mfac.getVM().currentHolderUriLiveData.postValue(item.uri)
        }
    }

    override fun onViewRecycled(holder: MediaFileViewHolder) {
        super.onViewRecycled(holder)
        holder.observerUri?.let { observer ->
            holder.selectHolder(false)
            mfac.getVM().currentHolderUriLiveData.removeObserver(observer)
            holder.observerUri = null
        }
    }

    override fun getItemCount() = itemList.size
}