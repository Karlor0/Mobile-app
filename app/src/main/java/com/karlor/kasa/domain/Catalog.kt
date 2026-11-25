package com.karlor.kasa.domain

object Catalog {
    val expenseCategories = listOf(
        "Market", "Fatura", "Kira", "Aidat", "Ulaşım", "Yakıt", "Yemek", "Sağlık", "Eğitim",
        "Eğlence", "Giyim", "Abonelik", "Sigorta", "Vergi", "Teknoloji", "Ev", "Bakım",
        "Hediye", "Evcil Hayvan", "Kredi Kartı", "Kredi", "Diğer",
    )

    val incomeCategories = listOf(
        "Maaş", "Ek Gelir", "Serbest İş", "Kira Geliri", "Yatırım", "Prim", "Burs", "İade", "Hediye", "Diğer",
    )

    const val CARD_PAYMENT = "Kredi Kartı"
    const val LOAN_PAYMENT = "Kredi"

    /** Banks and payment institutions operating in Türkiye (names only, for labelling). */
    val banks = listOf(
        "Ziraat Bankası", "Halkbank", "VakıfBank", "İş Bankası", "Garanti BBVA", "Akbank",
        "Yapı Kredi", "QNB", "DenizBank", "TEB", "ING", "Enpara.com", "HSBC", "Şekerbank",
        "Fibabanka", "Odeabank", "Alternatif Bank", "Anadolubank", "Burgan Bank", "ICBC Turkey",
        "Kuveyt Türk", "Türkiye Finans", "Albaraka Türk", "Ziraat Katılım", "Vakıf Katılım",
        "Emlak Katılım", "Hayat Finans", "Papara", "Diğer",
    )

    val loanTypes = listOf(
        "İhtiyaç Kredisi", "Konut Kredisi", "Taşıt Kredisi", "Kredili Mevduat (KMH)",
        "Eğitim Kredisi", "Ticari Kredi", "Taksitli Nakit Avans", "Diğer",
    )
}
