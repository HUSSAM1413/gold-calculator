package com.example.goldcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GoldCalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun GoldCalculatorScreen() {
    val scope = rememberCoroutineScope()
    val httpClient = remember { OkHttpClient() }

    var modeIndex by remember { mutableIntStateOf(0) } // 0: حساب الإجمالي، 1: تحليل سعر البائع
    var karat by remember { mutableIntStateOf(21) }
    var goldPricePerGram24 by remember { mutableStateOf("310.0") }
    var weightGrams by remember { mutableStateOf("10.0") }
    var makingChargePerGram by remember { mutableStateOf("25.0") }
    var sellerTotalPrice by remember { mutableStateOf("3200.0") }
    var statusMessage by remember { mutableStateOf("يمكنك تحديث سعر عيار 24 أو إدخاله يدوياً") }
    var isLoading by remember { mutableStateOf(false) }

    fun fetchGoldPrice() {
        isLoading = true
        statusMessage = "جاري جلب السعر..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val sources = listOf(
                    "https://xaus.com/",
                    "https://saudigoldprice.com/"
                )
                for (url in sources) {
                    try {
                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0")
                            .build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body?.string().orEmpty()
                                val match = Regex("""\b([234567]\d{2}(?:\.\d{1,2})?)\b""")
                                    .findAll(body)
                                    .mapNotNull { it.groupValues[1].toDoubleOrNull() }
                                    .firstOrNull { it in 200.0..600.0 }
                                if (match != null) {
                                    return@withContext Pair(match, url)
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                null
            }
            isLoading = false
            if (result != null) {
                goldPricePerGram24 = String.format(Locale.US, "%.2f", result.first)
                statusMessage = "تم التحديث من ${result.second}"
            } else {
                statusMessage = "تعذر الجلب تلقائياً، يمكنك إدخال السعر يدوياً"
            }
        }
    }

    val price24 = goldPricePerGram24.toDoubleOrNull() ?: 0.0
    val karatRatio = karat / 24.0
    val priceForKarat = price24 * karatRatio
    val weight = weightGrams.toDoubleOrNull() ?: 0.0
    val makingPerGram = makingChargePerGram.toDoubleOrNull() ?: 0.0
    val sellerPrice = sellerTotalPrice.toDoubleOrNull() ?: 0.0

    val pureGoldCost = priceForKarat * weight
    val makingTotal = makingPerGram * weight
    val estimatedTotal = pureGoldCost + makingTotal
    val impliedMakingPerGram = if (weight > 0.0) (sellerPrice - pureGoldCost) / weight else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "حاسبة الذهب",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = modeIndex == 0,
                onClick = { modeIndex = 0 },
                label = { Text("حساب الإجمالي") }
            )
            FilterChip(
                selected = modeIndex == 1,
                onClick = { modeIndex = 1 },
                label = { Text("تحليل سعر البائع") }
            )
        }

        Text(text = "اختر العيار:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(24, 22, 21, 18).forEach { k ->
                FilterChip(
                    selected = karat == k,
                    onClick = { karat = k },
                    label = { Text("عيار $k") }
                )
            }
        }

        OutlinedTextField(
            value = goldPricePerGram24,
            onValueChange = { goldPricePerGram24 = it },
            label = { Text("سعر جرام عيار 24") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { fetchGoldPrice() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "جاري الجلب..." else "تحديث السعر عبر الإنترنت")
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = weightGrams,
            onValueChange = { weightGrams = it },
            label = { Text("الوزن بالجرام") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (modeIndex == 0) {
            OutlinedTextField(
                value = makingChargePerGram,
                onValueChange = { makingChargePerGram = it },
                label = { Text("المصنعية لكل جرام") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = sellerTotalPrice,
                onValueChange = { sellerTotalPrice = it },
                label = { Text("السعر الإجمالي المطلوب من البائع") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "سعر جرام عيار $karat: ${String.format(Locale.US, "%.2f", priceForKarat)}",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "قيمة الذهب الصافي: ${String.format(Locale.US, "%.2f", pureGoldCost)}"
                )
                if (modeIndex == 0) {
                    Text(
                        text = "إجمالي المصنعية: ${String.format(Locale.US, "%.2f", makingTotal)}"
                    )
                    Text(
                        text = "الإجمالي المتوقع: ${String.format(Locale.US, "%.2f", estimatedTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "المصنعية التقريبية للجرام: ${String.format(Locale.US, "%.2f", impliedMakingPerGram)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
