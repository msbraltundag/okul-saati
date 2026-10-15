package com.ornek.okulzili

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class GunlukProgram(
    val dersSuresiDakika: Int,
    val ogleArasiDersNo: Int,
    val girisSaatleri: List<LocalTime>
)

data class ZamanDilimi(
    val baslik: String,
    val baslangic: LocalTime,
    val bitis: LocalTime
)

data class AktifDurum(
    val baslik: String,
    val altBilgi: String,
    val hedefZamanMillis: Long?,
    val sonrakiAlarmMillis: Long?
)

object ProgramYoneticisi {
    private const val PREF_NAME = "OkulZiliPrefs"
    private val saatFormati = DateTimeFormatter.ofPattern("HH:mm")

    fun programiKaydet(context: Context, gun: Int, program: GunlukProgram) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val saatlerStr = program.girisSaatleri.joinToString(",") { it.format(saatFormati) }
        val kayitMetni = "${program.dersSuresiDakika}|${program.ogleArasiDersNo}|$saatlerStr"
        prefs.edit().putString("gun_$gun", kayitMetni).apply()
    }

    fun programiGetir(context: Context, gun: Int): GunlukProgram {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val kayit = prefs.getString("gun_$gun", null)

        if (kayit.isNullOrBlank()) {
            val varsayilanSaatler = if (gun in 1..5) {
                listOf("08:30", "09:20", "10:10", "11:00", "12:30", "13:20", "14:10", "15:00")
                    .map { LocalTime.parse(it, saatFormati) }
            } else emptyList()
            return GunlukProgram(40, 4, varsayilanSaatler)
        }

        val parcalar = kayit.split("|")
        val sure = parcalar.getOrNull(0)?.toIntOrNull() ?: 40
        val ogle = parcalar.getOrNull(1)?.toIntOrNull() ?: 4
        val saatler = parcalar.getOrNull(2)?.split(",")
            ?.mapNotNull {
                try { LocalTime.parse(it.trim(), saatFormati) } catch (e: Exception) { null }
            }?.sorted() ?: emptyList()

        return GunlukProgram(sure, ogle, saatler)
    }

    fun haftaIcineKopyala(context: Context, kaynakProgram: GunlukProgram) {
        for (gun in 1..5) {
            programiKaydet(context, gun, kaynakProgram)
        }
    }

    fun dilimleriOlustur(program: GunlukProgram): List<ZamanDilimi> {
        val liste = mutableListOf<ZamanDilimi>()
        val saatler = program.girisSaatleri.sorted()

        for (i in saatler.indices) {
            val dersNo = i + 1
            val baslangic = saatler[i]
            val bitis = baslangic.plusMinutes(program.dersSuresiDakika.toLong())

            liste.add(ZamanDilimi("$dersNo. Ders", baslangic, bitis))

            if (i < saatler.size - 1) {
                val sonrakiBaslangic = saatler[i + 1]
                if (sonrakiBaslangic.isAfter(bitis)) {
                    val araAdi = if (dersNo == program.ogleArasiDersNo) "Öğle Arası" else "Teneffüs"
                    liste.add(ZamanDilimi(araAdi, bitis, sonrakiBaslangic))
                }
            }
        }
        return liste
    }

    fun suAnkiDurumuHesapla(context: Context): AktifDurum {
        val bugun = LocalDate.now()
        val gunNo = bugun.dayOfWeek.value
        val program = programiGetir(context, gunNo)
        val dilimler = dilimleriOlustur(program)
        val simdi = LocalTime.now()

        if (dilimler.isEmpty()) {
            return AktifDurum("Bugün Ders Yok", "İyi Tatiller", null, yarinSabahAlarmi())
        }

        val ilkDilim = dilimler.first()
        if (simdi.isBefore(ilkDilim.baslangic)) {
            val hedefMillis = bugun.atTime(ilkDilim.baslangic)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return AktifDurum(
                baslik = "1. Derse Kalan",
                altBilgi = "Başlangıç: ${ilkDilim.baslangic.format(saatFormati)}",
                hedefZamanMillis = hedefMillis,
                sonrakiAlarmMillis = hedefMillis
            )
        }

        for (dilim in dilimler) {
            if (!simdi.isBefore(dilim.baslangic) && simdi.isBefore(dilim.bitis)) {
                val hedefMillis = bugun.atTime(dilim.bitis)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val aralikMetni = "${dilim.baslangic.format(saatFormati)} - ${dilim.bitis.format(saatFormati)}"
                return AktifDurum(
                    baslik = dilim.baslik,
                    altBilgi = aralikMetni,
                    hedefZamanMillis = hedefMillis,
                    sonrakiAlarmMillis = hedefMillis
                )
            }
        }

        return AktifDurum("Okul Bitti", "İyi İstirahatler", null, yarinSabahAlarmi())
    }

    private fun yarinSabahAlarmi(): Long {
        return LocalDate.now().plusDays(1).atTime(7, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
