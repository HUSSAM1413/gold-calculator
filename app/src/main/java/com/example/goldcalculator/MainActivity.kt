package com.example.goldcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

private val BgDark = Color(0xFF14110B)
private val CardDark = Color(0xFF1E1910)
private val InputDark = Color(0xFF18140C)
private val BorderGoldDark = Color(0xFF3A2F1B)
private val GoldAccent = Color(0xFFE5B12B)
private val GoldLightCard = Color(0xFFF3D57B)
private val TextGold = Color(0xFFE5B12B)
private val TextLight = Color(0xFFF5EFE0)
private val TextMuted = Color(0xFFA89C82)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    GoldBuyCalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun GoldBuyCalculatorScreen() {
    val scope = rememberCoroutineScope()
    val httpClient = remember { OkHttpClient() }

    var modeIndex by remember { mutableIntStateOf(0) } // 0: احسب المجموع, 1: حلل سعر البائع
    var weightText by remember { mutableStateOf("1") }
    var selectedKarat by remember { mutableIntStateOf(24) }
    var pricePerGramText by remember { mutableStateOf("0") }
    var makingPerGramText by remember { mutableStateOf("0") }
    var shopMarginPerGramText by remember { mutableStateOf("0") }
    var sellerTotalText by remember { mutableStateOf("0") }
    var is24kBullionNoVat by remember { mutableStateOf(false) }
    var statusNote by remember { mutableStateOf("اضغط على العيار عشان يجيب سعر اليوم") }
    var isFetching by remember { mutableStateOf(false) }

    fun fetchPriceForKarat(targetKarat: Int) {
        selectedKarat = targetKarat
        isFetching = true
        statusNote = "جاري جلب سعر عيار $targetKarat..."
        scope.launch {
            val price24Result = withContext(Dispatchers.IO) {
                val urls = listOf("https://xaus.com/", "https://saudigoldprice.com/")
                for (url in urls) {
                    try {
                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0")
                            .build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val html = resp.body?.string().orEmpty()
                                val found = Regex("""\b([234567]\d{2}(?:\.\d{1,2})?)\b""")
                                    .findAll(html)
                                    .mapNotNull { it.groupValues[1].toDoubleOrNull() }
                                    .firstOrNull { it in 200.0..600.0 }
                                if (found != null) return@withContext found
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                null
            }
            isFetching = false
            if (price24Result != null) {
                val calculated = price24Result * (targetKarat / 24.0)
                pricePerGramText = String.format(Locale.US, "%.2f", calculated)
                statusNote = "تم جلب سعر عيار $targetKarat تلقائياً"
            } else {
                statusNote = "تعذر الجلب تلقائياً، يمكنك إدخال السعر يدوياً"
            }
        }
    }

    fun resetAll() {
        modeIndex = 0
        weightText = "1"
        selectedKarat = 24
        pricePerGramText = "0"
        makingPerGramText = "0"
        shopMarginPerGramText = "0"
        sellerTotalText = "0"
        is24kBullionNoVat = false
        statusNote = "اضغط على العيار عشان يجيب سعر اليوم"
    }

    val weight = weightText.toDoubleOrNull() ?: 0.0
    val gramPrice = pricePerGramText.toDoubleOrNull() ?: 0.0
    val makingPerGram = makingPerGramText.toDoubleOrNull() ?: 0.0
    val marginPerGram = shopMarginPerGramText.toDoubleOrNull() ?: 0.0
    val sellerTotal = sellerTotalText.toDoubleOrNull() ?: 0.0

    val goldValue = gramPrice * weight
    val makingTotal = makingPerGram * weight
    val marginTotal = marginPerGram * weight

    val subtotalBeforeVat = if (modeIndex == 0) {
        goldValue + makingTotal + marginTotal
    } else {
        sellerTotal / 1.15
    }

    val vatAmount = if (modeIndex == 0) {
        if (is24kBullionNoVat && selectedKarat == 24) {
            (makingTotal + marginTotal) * 0.15
        } else {
            subtotalBeforeVat * 0.15
        }
    } else {
        sellerTotal - subtotalBeforeVat
    }

    val totalWithVat = if (modeIndex == 0) {
        subtotalBeforeVat + vatAmount
    } else {
        sellerTotal
    }

    val impliedMakingAndMarginPerGram = if (weight > 0.0) {
        ((subtotalBeforeVat - goldValue) / weight).coerceAtLeast(0.0)
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "حاسبة شراء الذهب",
            color = TextGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // أزرار الوضع العلوي
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeButton(
                text = "احسب المجموع",
                selected = modeIndex == 0,
                onClick = { modeIndex = 0 },
                modifier = Modifier.weight(1f)
            )
            ModeButton(
                text = "حلل سعر البائع",
                selected = modeIndex == 1,
                onClick = { modeIndex = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        // البطاقة الرئيسية
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, BorderGoldDark, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FieldLabel("الوزن")
            GoldInputBox(
                value = weightText,
                onValueChange = { weightText = it },
                suffix = "جرام"
            )

            FieldLabel("سعر الجرام")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(24, 22, 21, 18).forEach { karat ->
                    val isSelected = selectedKarat == karat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GoldAccent else InputDark)
                            .border(
                                1.dp,
                                if (isSelected) GoldAccent else BorderGoldDark,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { fetchPriceForKarat(karat) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$karat",
                            color = if (isSelected) BgDark else TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            GoldInputBox(
                value = pricePerGramText,
                onValueChange = { pricePerGramText = it },
                suffix = "ريال"
            )

            Text(
                text = if (isFetching) "جاري جلب سعر اليوم..." else statusNote,
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (modeIndex == 0) {
                FieldLabel("المصنعية")
                GoldInputBox(
                    value = makingPerGramText,
                    onValueChange = { makingPerGramText = it },
                    suffix = "ريال / جرام"
                )

                FieldLabel("مكسب المحل")
                GoldInputBox(
                    value = shopMarginPerGramText,
                    onValueChange = { shopMarginPerGramText = it },
                    suffix = "ريال / جرام"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { is24kBullionNoVat = !is24kBullionNoVat }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(if (is24kBullionNoVat) GoldAccent else Color.White, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (is24kBullionNoVat) {
                            Text("✓", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Text(
                        text = "سبائك عيار 24 (بدون ضريبة)",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                FieldLabel("سعر البائع الإجمالي (شامل الضريبة)")
                GoldInputBox(
                    value = sellerTotalText,
                    onValueChange = { sellerTotalText = it },
                    suffix = "ريال"
                )
            }

            // بطاقة المجموع الذهبية البارزة
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GoldLightCard)
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (modeIndex == 0) "المجموع شامل الضريبة" else "مصنعية + مكسب المحل التقريبي / جرام",
                    color = BgDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%.2f",
                        if (modeIndex == 0) totalWithVat else impliedMakingAndMarginPerGram
                    ),
                    color = BgDark,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // ملخص التفاصيل
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryRow("قيمة الذهب", String.format(Locale.US, "%.2f", goldValue))
                SummaryRow("المصنعية", String.format(Locale.US, "%.2f", makingTotal))
                SummaryRow("مكسب المحل", String.format(Locale.US, "%.2f", marginTotal))
                SummaryRow("المجموع قبل الضريبة", String.format(Locale.US, "%.2f", subtotalBeforeVat))
                SummaryRow("ضريبة القيمة المضافة 15%", String.format(Locale.US, "%.2f", vatAmount))
            }

            // زر مسح الكل
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(InputDark)
                    .border(1.dp, BorderGoldDark, RoundedCornerShape(10.dp))
                    .clickable { resetAll() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "مسح الكل",
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GoldAccent else CardDark)
            .border(
                BorderStroke(1.dp, if (selected) GoldAccent else BorderGoldDark),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) BgDark else TextGold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun GoldInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InputDark)
            .border(1.dp, BorderGoldDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = suffix,
            color = TextMuted,
            fontSize = 13.sp
        )
        BasicGoldNumberField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BasicGoldNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { newVal ->
            if (newVal.isEmpty() || newVal.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                onValueChange(newVal)
            }
        },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            color = TextLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.padding(start = 8.dp)
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = TextLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
