package com.fourbrothers.protector

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fourbrothers.protector.databinding.ItemProtectorBinding

class ProtectorAdapter(
    private var items: List<Protector>,
    private val onShare: (Protector) -> Unit,
    private val onEdit: (Protector) -> Unit,
    private val onDelete: (Protector) -> Unit
) : RecyclerView.Adapter<ProtectorAdapter.VH>() {

    class VH(val binding: ItemProtectorBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<Protector>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProtectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val count = item.mobileModels.size
        holder.binding.title.text = item.protectorName
        holder.binding.modelCount.text =
            if (count == 1) "1 mobile model" else "$count mobile models"
        holder.binding.models.text = item.mobileModels.joinToString("\n") { "•  $it" }
        holder.binding.btnShare.setOnClickListener { onShare(item) }
        holder.binding.btnEdit.setOnClickListener { onEdit(item) }
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }
}
