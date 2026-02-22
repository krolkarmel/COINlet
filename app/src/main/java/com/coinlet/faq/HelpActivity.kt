package com.coinlet.faq

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.coinlet.R
import com.coinlet.app.faq.FaqAdapter
import com.coinlet.faq.FaqItem
import com.coinlet.databinding.ActivityContactBinding
import com.coinlet.databinding.ActivityHelpBinding
import kotlin.collections.filter

class HelpActivity : AppCompatActivity() {

    private lateinit var adapter: FaqAdapter
    private lateinit var allFaq: List<FaqItem>
    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }



        allFaq = listOf(
            FaqItem("Jak zresetować PIN?", "Ustawienia → Blokada aplikacji → Zmień PIN."),
            FaqItem(
                "Nie działa logowanie biometryczne",
                "Włącz biometrię w systemie i dodaj odcisk/twarz."
            ),
            FaqItem("Jak wykonać przelew?", "Dashboard → Przelewy → uzupełnij dane i wyślij."),
            FaqItem(
                "Aplikacja się wyłącza",
                "Zaktualizuj aplikację i spróbuj ponownie; jeśli problem trwa, skontaktuj się z supportem."
            )
        )

        adapter = FaqAdapter(allFaq)

        findViewById<RecyclerView>(R.id.rvFaq).apply {
            layoutManager = LinearLayoutManager(this@HelpActivity)
            adapter = this@HelpActivity.adapter
        }

        findViewById<SearchView>(R.id.searchFaq)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    val q = newText.orEmpty().trim().lowercase()
                    val filtered = if (q.isEmpty()) allFaq else allFaq.filter {
                        it.question.lowercase().contains(q) || it.answer.lowercase().contains(q)
                    }.map { it.copy(expanded = false) }

                    adapter.submitList(filtered)
                    return true
                }
            })
    }
}