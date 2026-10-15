package com.ornek.okulzili

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerGunler: Spinner
    private lateinit var etDersSuresi: EditText
    private lateinit var etOgleArasi: EditText
    private lateinit var etGirisSaatleri: EditText
    private lateinit var tvOnizleme: TextView
    private val saatFormati = DateTimeFormatter.ofPattern("HH:mm")
    private var seciliGun = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerGunler = findViewById(R.id.spinnerGunler)
        etDersSuresi = findViewById(R.id.etDersSuresi)
        etOgleArasi = findViewById(R.id.etOgleArasi)
        etGirisSaatleri = findViewById(R.id.etGirisSaatleri)
        tvOnizleme = findViewById(R.id.tvOnizleme)

        val btnKaydet: Button = findViewById(R.id.btnKaydet)
        val btnHaftaIcineKopyala: Button = findViewById(R.id.btnHaftaIcineKopyala)

        alarmIzniniKontrolEt()

        val gunIsimleri = arrayOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
        spinnerGunler.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gunIsimleri)

        spinnerGunler.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                seciliGun = position + 1
                gunVerileriniYukle(seciliGun)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnKaydet.setOnClickListener {
            val program = ekrandakiVerileriOku() ?: return@setOnClickListener
            ProgramYoneticisi.programiKaydet(this, seciliGun, program)
            onizlemeyiGuncelle(program)
            ZilWidgetProvider.tumWidgetlariGuncelle(this)
            Toast.makeText(this, "Seçili gün kaydedildi ve Widget güncellendi!", Toast.LENGTH_SHORT).show()
        }

        btnHaftaIcineKopyala.setOnClickListener {
            val program = ekrandakiVerileriOku() ?: return@setOnClickListener
            ProgramYoneticisi.haftaIcineKopyala(this, program)
            onizlemeyiGuncelle(program)
            ZilWidgetProvider.tumWidgetlariGuncelle(this)
            Toast.makeText(this, "Bu program Pazartesi-Cuma tüm günlere kopyalandı!", Toast.LENGTH_LONG).show()
        }
    }

    private fun gunVerileriniYukle(gun: Int) {
        val program = ProgramYoneticisi.programiGetir(this, gun)
        etDersSuresi.setText(program.dersSuresiDakika.toString())
        etOgleArasi.setText(program.ogleArasiDersNo.toString())
        etGirisSaatleri.setText(program.girisSaatleri.joinToString("\n") { it.format(saatFormati) })
        onizlemeyiGuncelle(program)
    }

    private fun ekrandakiVerileriOku(): GunlukProgram? {
        val dersSuresi = etDersSuresi.text.toString().toIntOrNull()
        if (dersSuresi == null || dersSuresi <= 0) {
            Toast.makeText(this, "Geçerli bir ders süresi girin!", Toast.LENGTH_SHORT).show()
            return null
        }

        val ogleArasi = etOgleArasi.text.toString().toIntOrNull() ?: 0
        val hamMetin = etGirisSaatleri.text.toString()

        val saatListesi = mutableListOf<LocalTime>()
        val parcalar = hamMetin.split("\n", ",", ";", " ").map { it.trim() }.filter { it.isNotEmpty() }

        for (parca in parcalar) {
            try {
                val duzenlenmis = if (parca.length == 4 && parca[1] == ':') "0$parca" else parca
                saatListesi.add(LocalTime.parse(duzenlenmis, saatFormati))
            } catch (e: Exception) {
                Toast.makeText(this, "Hatalı saat formatı: '$parca' (Örn: 08:30 şeklinde yazın)", Toast.LENGTH_LONG).show()
                return null
            }
        }

        return GunlukProgram(dersSuresi, ogleArasi, saatListesi.sorted())
    }

    private fun onizlemeyiGuncelle(program: GunlukProgram) {
        val dilimler = ProgramYoneticisi.dilimleriOlustur(program)
        if (dilimler.isEmpty()) {
            tvOnizleme.text = "Bu gün için ders saati girilmedi."
            return
        }

        val sb = StringBuilder()
        for (d in dilimler) {
            val sureDk = java.time.Duration.between(d.baslangic, d.bitis).toMinutes()
            sb.append("• ${d.baslik}: ${d.baslangic.format(saatFormati)} - ${d.bitis.format(saatFormati)} ($sureDk dk)\n")
        }
        tvOnizleme.text = sb.toString()
    }

    private fun alarmIzniniKontrolEt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Zillerin tam dakikasında değişmesi için alarm izni verin.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
