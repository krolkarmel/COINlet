package com.coinlet.app.faq

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.coinlet.R

class FaqAdapter(
    private var items: List<FaqItem>
) : RecyclerView.Adapter<FaqAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val tvAnswer: TextView = view.findViewById(R.id.tvAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvQuestion.text = item.question
        holder.tvAnswer.text = item.answer
        holder.tvAnswer.visibility = if (item.expanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            item.expanded = !item.expanded
            notifyItemChanged(position)
        }
    }

    fun submitList(newItems: List<FaqItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}