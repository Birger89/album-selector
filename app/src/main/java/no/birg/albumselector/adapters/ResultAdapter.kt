package no.birg.albumselector.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.result_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.SearchFragment
import no.birg.albumselector.database.Album
import org.json.JSONArray
import org.json.JSONObject

class ResultAdapter(context: Context, private val results: JSONArray, fragment: SearchFragment) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mFragment: SearchFragment = fragment
    private val mContext = context

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

        holder.titleTextView.text = title

        holder.addButton.setOnClickListener {
            mFragment.addAlbum(id, title)
            holder.addButton.setTextColor(ContextCompat.getColor(mContext, R.color.spotifyGreen))
        }

        GlobalScope.launch(Dispatchers.Default) {
            val inLibrary = mFragment.checkRecord(id)
            withContext(Dispatchers.Main) {
                if (inLibrary) {
                    holder.addButton.setTextColor(ContextCompat.getColor(mContext, R.color.spotifyGreen))
                } else {
                    holder.addButton.setTextColor(ContextCompat.getColor(mContext, android.R.color.white))
                }
            }
        }

        resultView.setOnClickListener {
            val album = Album(id, title)
            mFragment.displayAlbumDetails(album)
        }

        return resultView
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
        lateinit var addButton: Button
    }
}
