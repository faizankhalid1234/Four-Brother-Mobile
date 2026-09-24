package com.fourbrothers.protector

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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_suggestion, parent, false)
        val item = getItem(position) ?: return view
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
            return FilterResults().apply {
                values = items
                count = items.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }
}
