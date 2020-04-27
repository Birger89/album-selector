package no.birg.albumselector

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.result_item.view.*
import org.json.JSONArray
import org.json.JSONObject

class ResultAdapter(context: Context, private val results: JSONArray, fragment: SearchFragment) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mFragment: SearchFragment = fragment

    override fun getCount(): Int {
        return results.length()
    }

    override fun getItem(position: Int): Any {
        return results[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val resultView: View
        val holder: ViewHolder

        if (convertView == null) {
            resultView = inflater.inflate(R.layout.result_item, parent, false)

            holder = ViewHolder()
            holder.titleTextView = resultView.album_title as TextView
            holder.addButton = resultView.add_button as Button

            resultView.tag = holder
        } else {
            resultView = convertView
            holder = convertView.tag as ViewHolder
        }

        val result = getItem(position) as JSONObject
        val title = result.getString("name")
        val id = result.getString("id")
        val uri = result.getString("uri")

        holder.titleTextView.text = title

        holder.addButton.setOnClickListener {
            mFragment.addAlbum(id, title, uri)
        }
        resultView.setOnClickListener {
            val album = Album(id, title, uri)
            mFragment.displayAlbumDetails(album)
        }

        return resultView
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
        lateinit var addButton: Button
    }
}
