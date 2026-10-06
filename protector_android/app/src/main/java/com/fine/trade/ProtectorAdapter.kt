package com.fine.trade

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fine.trade.databinding.ItemProtectorBinding

class ProtectorAdapter(
    private var items: List<Protector>,
    private val onShare: (Protector) -> Unit,
    private val onEdit: (Protector) -> Unit,
    private val onDelete: (Protector) -> Unit
) : RecyclerView.Adapter<ProtectorAdapter.VH>() {

    private val expandedIds = mutableSetOf<String>()

    class VH(val binding: ItemProtectorBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<Protector>) {
        items = list
        expandedIds.retainAll(list.map { it.id }.toSet())
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
        bindModelGrid(holder.binding, item.mobileModels)
        applyExpanded(holder.binding, item.id, animate = false)
        holder.binding.headerRow.setOnClickListener {
            toggleExpanded(item.id, holder.binding)
        }
        holder.binding.btnShare.setOnClickListener { onShare(item) }
        holder.binding.btnEdit.setOnClickListener { onEdit(item) }
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    private fun bindModelGrid(binding: ItemProtectorBinding, models: List<String>) {
        val grid = binding.modelsGrid
        grid.removeAllViews()
        val inflater = LayoutInflater.from(grid.context)
        val gap = (8 * grid.resources.displayMetrics.density).toInt()
        val numberWidth = models.size.toString().length
        models.forEachIndexed { index, name ->
            val cell = inflater.inflate(R.layout.item_model_cell, grid, false) as TextView
            val number = (index + 1).toString().padStart(numberWidth, '\u2007')
            cell.text = "$number.  $name"
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                val end = if (index % 2 == 0) gap else 0
                val start = if (index % 2 == 1) gap else 0
                setMargins(start, 0, end, 0)
            }
            grid.addView(cell, params)
        }
    }

    private fun toggleExpanded(id: String, binding: ItemProtectorBinding) {
        if (!expandedIds.add(id)) {
            expandedIds.remove(id)
        }
        applyExpanded(binding, id, animate = true)
    }

    private fun applyExpanded(
        binding: ItemProtectorBinding,
        id: String,
        animate: Boolean
    ) {
        val expanded = id in expandedIds
        binding.detailsPanel.visibility = if (expanded) View.VISIBLE else View.GONE
        val rotation = if (expanded) 180f else 0f
        binding.btnExpand.animate().cancel()
        if (animate) {
            binding.btnExpand.animate().rotation(rotation).setDuration(180).start()
        } else {
            binding.btnExpand.rotation = rotation
        }
        binding.btnExpand.contentDescription = binding.root.context.getString(
            if (expanded) R.string.collapse_card else R.string.expand_card
        )
    }
}
