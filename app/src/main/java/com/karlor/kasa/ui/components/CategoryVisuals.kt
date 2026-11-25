package com.karlor.kasa.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryVisual(val icon: ImageVector, val color: Color)

object CategoryVisuals {
    private val map: Map<String, CategoryVisual> = mapOf(
        "Market" to CategoryVisual(Icons.Rounded.ShoppingCart, Color(0xFF2DD4BF)),
        "Fatura" to CategoryVisual(Icons.Rounded.Bolt, Color(0xFFFFB547)),
        "Kira" to CategoryVisual(Icons.Rounded.House, Color(0xFF7C6CFF)),
        "Aidat" to CategoryVisual(Icons.Rounded.Apartment, Color(0xFFA78BFA)),
        "Ulaşım" to CategoryVisual(Icons.Rounded.DirectionsBus, Color(0xFF38BDF8)),
        "Yakıt" to CategoryVisual(Icons.Rounded.LocalGasStation, Color(0xFFFB923C)),
        "Yemek" to CategoryVisual(Icons.Rounded.Restaurant, Color(0xFFFF7A90)),
        "Sağlık" to CategoryVisual(Icons.Rounded.LocalHospital, Color(0xFFF43F5E)),
        "Eğitim" to CategoryVisual(Icons.Rounded.School, Color(0xFF60A5FA)),
        "Eğlence" to CategoryVisual(Icons.Rounded.Celebration, Color(0xFFF472B6)),
        "Giyim" to CategoryVisual(Icons.Rounded.Checkroom, Color(0xFFC084FC)),
        "Abonelik" to CategoryVisual(Icons.Rounded.Subscriptions, Color(0xFFEF4444)),
        "Sigorta" to CategoryVisual(Icons.Rounded.HealthAndSafety, Color(0xFF22C55E)),
        "Vergi" to CategoryVisual(Icons.Rounded.AccountBalance, Color(0xFF94A3B8)),
        "Teknoloji" to CategoryVisual(Icons.Rounded.Devices, Color(0xFF06B6D4)),
        "Ev" to CategoryVisual(Icons.Rounded.Weekend, Color(0xFFEAB308)),
        "Bakım" to CategoryVisual(Icons.Rounded.Build, Color(0xFF78716C)),
        "Hediye" to CategoryVisual(Icons.Rounded.CardGiftcard, Color(0xFFEC4899)),
        "Evcil Hayvan" to CategoryVisual(Icons.Rounded.Pets, Color(0xFFD97706)),
        "Kredi Kartı" to CategoryVisual(Icons.Rounded.CreditCard, Color(0xFF6366F1)),
        "Kredi" to CategoryVisual(Icons.Rounded.AccountBalanceWallet, Color(0xFF8B5CF6)),
        "Maaş" to CategoryVisual(Icons.Rounded.Work, Color(0xFF34E0A1)),
        "Ek Gelir" to CategoryVisual(Icons.Rounded.AddCard, Color(0xFF2DD4BF)),
        "Serbest İş" to CategoryVisual(Icons.Rounded.Laptop, Color(0xFF38BDF8)),
        "Kira Geliri" to CategoryVisual(Icons.Rounded.Key, Color(0xFFA78BFA)),
        "Yatırım" to CategoryVisual(Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFF4ADE80)),
        "Prim" to CategoryVisual(Icons.Rounded.EmojiEvents, Color(0xFFFFB547)),
        "Burs" to CategoryVisual(Icons.Rounded.School, Color(0xFF60A5FA)),
        "İade" to CategoryVisual(Icons.Rounded.Replay, Color(0xFF94A3B8)),
    )
    private val fallback = CategoryVisual(Icons.AutoMirrored.Rounded.ReceiptLong, Color(0xFF94A3B8))

    fun of(category: String): CategoryVisual = map[category] ?: fallback
}
