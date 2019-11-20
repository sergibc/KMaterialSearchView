package com.sergibc.kmaterialsearchview

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*


class SearchAdapter : BaseAdapter, Filterable {

    private var data: ArrayList<String>? = null
    private var suggestions: Array<String>? = null
    private var suggestionIcon: Drawable? = null
    private var inflater: LayoutInflater? = null
    private var ellipsize: Boolean = false

    constructor(context: Context, suggestions: Array<String>) {
        inflater = LayoutInflater.from(context)
        data = ArrayList()
        this.suggestions = suggestions
    }

    constructor(
        context: Context,
        suggestions: Array<String>,
        suggestionIcon: Drawable?,
        ellipsize: Boolean
    ) {
        inflater = LayoutInflater.from(context)
        data = ArrayList()
        this.suggestions = suggestions
        this.suggestionIcon = suggestionIcon
        this.ellipsize = ellipsize
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val filterResults = FilterResults()
                if (!TextUtils.isEmpty(constraint)) {

                    // Retrieve the autocomplete results.
                    val searchData = ArrayList<String>()

                    for (string in suggestions!!) {
                        if (string.toLowerCase().startsWith(constraint.toString().trim().toLowerCase())) {
                            searchData.add(string)
                        }
                    }

                    // Assign the data to the FilterResults
                    filterResults.values = searchData
                    filterResults.count = searchData.size
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                if (results.values != null) {
                    data = results.values as ArrayList<String>
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getCount(): Int {
        return data!!.size
    }

    override fun getItem(position: Int): Any {
        return data!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val viewHolder: SuggestionsViewHolder

        if (convertView == null) {
            convertView = inflater!!.inflate(R.layout.suggest_item, parent, false)
            viewHolder = SuggestionsViewHolder(convertView!!)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as SuggestionsViewHolder
        }

        val currentListData = getItem(position) as String

        viewHolder.textView?.text = currentListData
        if (ellipsize) {
            viewHolder.textView?.setSingleLine()
            viewHolder.textView?.ellipsize = TextUtils.TruncateAt.END
        }

        return convertView
    }

    private inner class SuggestionsViewHolder(convertView: View) {

        internal var textView: TextView? = null
        internal var imageView: ImageView? = null

        init {
            textView = convertView.findViewById(R.id.suggestion_text) as TextView
            if (suggestionIcon != null) {
                imageView = convertView.findViewById(R.id.suggestion_icon) as ImageView
                imageView?.setImageDrawable(suggestionIcon)
            }
        }
    }
}