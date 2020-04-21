package no.birg.albumselector

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.device_item.view.*

class DeviceAdapter(context: Context, private val devices: MutableList<Pair<String, String>>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return devices.size
    }

    override fun getItem(position: Int): Pair<String, String> {
        return devices[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder") // There is never going to be that many devices
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val deviceView = inflater.inflate(R.layout.device_item, parent, false)
        val device = getItem(position)

        deviceView.tag = device.first
        val nameTextView = deviceView.device_name as TextView
        nameTextView.text = device.second

        return deviceView
    }
}