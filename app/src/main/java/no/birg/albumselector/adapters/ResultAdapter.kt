package no.birg.albumselector.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.result_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.screens.search.SearchViewModel
import org.json.JSONArray
import org.json.JSONObject

class ResultAdapter(
    private val context: Context,
    private val results: JSONArray,
    private val viewModel: SearchViewModel
) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
        val imageUrl = result.getJSONArray("images")
            .getJSONObject(0).getString("url")

        var artistName = "No Artist Info"
        val artists = result.getJSONArray("artists")
        if (artists.length() == 1) {
            val artist = artists.getJSONObject(0)
            artistName = artist.getString("name")
        } else if (artists.length() > 1) {
            artistName = "Several Artists"
        }

        holder.titleTextView.text = title

        holder.addButton.setOnClickListener {
            addAlbum(Album(id, title, artistName, 0, imageUrl))
            holder.addButton.setTextColor(ContextCompat.getColor(context, R.color.spotifyGreen))
        }

        GlobalScope.launch {
            val inLibrary = viewModel.checkForAlbum(id)
            withContext(Dispatchers.Main) {
                if (inLibrary) {
                    holder.addButton.setTextColor(ContextCompat.getColor(context, R.color.spotifyGreen))
                } else {
                    holder.addButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
        }

        resultView.setOnClickListener {
            viewModel.selectAlbum(Album(id, title, artistName, 0, imageUrl))
            resultView.findNavController().navigate(R.id.action_searchFragment_to_resultDetailsFragment)
        }

        return resultView
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
        lateinit var addButton: Button
    }

    private fun addAlbum(album: Album) {
        viewModel.addAlbum(album)
    }
}
