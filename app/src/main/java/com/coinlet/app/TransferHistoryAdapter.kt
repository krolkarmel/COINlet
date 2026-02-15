package com.coinlet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.coinlet.R
import com.coinlet.model.Transactions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransferHistoryAdapter : RecyclerView.Adapter<TransferHistoryAdapter.VH>() {

    private val items = mutableListOf<Transactions>()
    private val df = SimpleDateFormat("dd.MM.yyyy", Locale("pl", "PL"))

    fun setData(newItems: List<Transactions>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], df)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRecipient = itemView.findViewById<TextView>(R.id.tvRecipient)
        private val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        private val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)

        fun bind(t: Transactions, df: SimpleDateFormat) {
            tvRecipient.text = t.receiverName
            tvTitle.text = "Tytuł: ${t.title}"

            val sign = if (t.type == "outgoing") "-" else "+"
            tvAmount.text = "$sign ${"%.2f".format(t.amount)} PLN"

            tvStatus.text = if (t.type == "outgoing") "Wysłany" else "Otrzymany"
            tvDate.text = df.format(Date(t.date))
        }
    }
}
