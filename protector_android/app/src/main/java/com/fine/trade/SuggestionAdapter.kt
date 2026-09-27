package com.fine.trade

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView

class SuggestionAdapter(
    context: Context,
    private val items: MutableList<SuggestionItem> = mutableListOf()
) : ArrayAdapter<SuggestionItem>(context, 0, items) {

    fun replaceAll(newItems: List<SuggestionItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): SuggestionItem? =
        items.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_suggestion, parent, false)
        val item = getItem(position) ?: return view
        view.tag = item
        view.findViewById<TextView>(R.id.suggestionText).text = item.label
        val kind = view.findViewById<TextView>(R.id.suggestionKind)
        kind.text = item.kindLabel
        kind.setBackgroundResource(
            when (item.kind) {
                SuggestionItem.Kind.PROTECTOR -> R.drawable.bg_kind_protector
                SuggestionItem.Kind.MODEL -> R.drawable.bg_kind_model
                SuggestionItem.Kind.BOTH -> R.drawable.bg_kind_both
            }
        )
        return view
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            // Suggestions are already filtered in StorageRepository; pass through.
            val snapshot = items.toList()
            return FilterResults().apply {
                values = snapshot
                count = snapshot.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }
}
