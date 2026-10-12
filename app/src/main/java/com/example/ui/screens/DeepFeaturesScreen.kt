package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DangerLevel
import com.example.data.model.DeepCategory
import com.example.data.model.DeepDangerousFeature
import com.example.data.model.DeepFeaturesRegistry
import com.example.data.model.ExecutionResult
import com.example.ui.components.CyberCard
import com.example.ui.components.DeepFeatureWarningDialog
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusBackground
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.theme.NexusViolet
import com.example.ui.viewmodel.NexusViewModel

@Composable
fun DeepFeaturesScreen(
    viewModel: NexusViewModel,
    modifier: Modifier = Modifier
) {
    val activeWarningFeature by viewModel.activeDeepFeatureWarning.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.deepFeatureFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.deepFeatureSearch.collectAsStateWithLifecycle()
    val lastExecutionResult by viewModel.lastDeepExecutionResult.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecutingDeepFeature.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()

    var selectedDangerFilter by remember { mutableStateOf<DangerLevel?>(null) }

    val filteredFeatures by remember(searchQuery, selectedCategory, selectedDangerFilter) {
        derivedStateOf {
            DeepFeaturesRegistry.allFeatures.filter { feature ->
                val matchesCategory = selectedCategory == null || feature.category == selectedCategory
                val matchesDanger = selectedDangerFilter == null || feature.dangerLevel == selectedDangerFilter
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim()
                    // Search by number like "42" or "#42" or keywords
                    val numberMatch = q.toIntOrNull()?.let { it == feature.number } ?: false
                    val hashNumberMatch = q.removePrefix("#").toIntOrNull()?.let { it == feature.number } ?: false

                    numberMatch || hashNumberMatch ||
                            feature.titleAr.contains(q, ignoreCase = true) ||
                            feature.titleEn.contains(q, ignoreCase = true) ||
                            feature.descriptionAr.contains(q, ignoreCase = true) ||
                            feature.command.contains(q, ignoreCase = true) ||
                            feature.warningAr.contains(q, ignoreCase = true)
                }
                matchesCategory && matchesDanger && matchesSearch
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBackground)
            .testTag("deep_features_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // Header Hero Banner
            item {
                DeepFeaturesHeroBanner(
                    totalCount = DeepFeaturesRegistry.allFeatures.size,
                    filteredCount = filteredFeatures.size,
                    isExecuting = isExecuting
                )
            }

            // Search Bar & Clear
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setDeepFeatureSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deep_feature_search_input"),
                    placeholder = {
                        Text(
                            text = "ابحث بالرقم (مثال: 42) أو بالاسم أو بالنواة أو البطارية...",
                            color = NexusTextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NexusCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setDeepFeatureSearch("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = NexusTextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusCyan,
                        unfocusedBorderColor = NexusCardBorder,
                        focusedContainerColor = NexusSurface,
                        unfocusedContainerColor = NexusSurface,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Horizontal Categories Selector
            item {
                DeepCategoriesSelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setDeepFeatureCategory(it) }
                )
            }

            // Danger Level Quick Filters
            item {
                DangerFilterChips(
                    selectedDanger = selectedDangerFilter,
                    onDangerSelected = { selectedDangerFilter = it }
                )
            }

            // Features List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة الميزات (${filteredFeatures.size} من 100)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary
                    )
                    Text(
                        text = "تحذير إلزامي قبل التنفيذ ⚠️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexusAmber
                    )
                }
            }

            // Feature Items
            if (filteredFeatures.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        color = NexusSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NexusCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = NexusTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لم يتم العثور على ميزات مطابقة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "جرّب تغيير فئة البحث أو مسح نص الاستعلام",
                                fontSize = 12.sp,
                                color = NexusTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredFeatures,
                    key = { it.id }
                ) { feature ->
                    DeepFeatureCard(
                        feature = feature,
                        onRunClick = {
                            viewModel.openDeepFeatureWarning(feature)
                        }
                    )
                }
            }
        }

        // Warning Dialog Before Execution
        activeWarningFeature?.let { feature ->
            DeepFeatureWarningDialog(
                feature = feature,
                targetPackage = selectedApp?.packageName,
                onConfirm = { commandToRun ->
                    viewModel.executeDeepFeature(
                        feature = feature,
                        commandToRun = commandToRun,
                        targetPackage = selectedApp?.packageName
                    )
                },
                onDismiss = {
                    viewModel.closeDeepFeatureWarning()
                }
            )
        }

        // Result Dialog after Execution
        lastExecutionResult?.let { result ->
            DeepExecutionResultDialog(
                result = result,
                onDismiss = { viewModel.dismissDeepExecutionResult() }
            )
        }
    }
}

@Composable
private fun DeepFeaturesHeroBanner(
    totalCount: Int,
    filteredCount: Int,
    isExecuting: Boolean
) {
    CyberCard(
        borderColor = NexusRed,
        backgroundColor = Color(0xFF0F0B13)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NexusRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, NexusRed, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dangerous,
                            contentDescription = null,
                            tint = NexusRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DEEP CONTROL MATRIX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = NexusRed,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "مصفوفة الـ 100 ميزة الفائقة والخطيرة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NexusTextPrimary
                        )
                    }
                }

                Surface(
                    color = NexusRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, NexusRed)
                ) {
                    Text(
                        text = "100 / 100",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = NexusRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "تحتوي هذه المصفوفة على 100 ميزة نظام عميقة وخارقة موزعة على 10 قطاعات حساسة في أندرويد ونواة لينكس. تتطلب كل ميزة تأكيداً ومراجعة للتحذير الأمني قبل تشغيلها لحماية الجهاز.",
                fontSize = 11.sp,
                color = NexusTextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Danger summary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickStatChip(label = "خطر فائق", count = 8, color = Color(0xFFFF1744), modifier = Modifier.weight(1f))
                QuickStatChip(label = "خطر حرج", count = 38, color = Color(0xFFFF5252), modifier = Modifier.weight(1f))
                QuickStatChip(label = "خطر مرتفع", count = 46, color = Color(0xFFFF9100), modifier = Modifier.weight(1f))
                QuickStatChip(label = "تعديل شديد", count = 8, color = Color(0xFFFFD600), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickStatChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DeepCategoriesSelector(
    selectedCategory: DeepCategory?,
    onCategorySelected: (DeepCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" Chip
        CategoryFilterChip(
            title = "الكل (100)",
            isSelected = selectedCategory == null,
            onClick = { onCategorySelected(null) }
        )

        DeepCategory.values().forEach { cat ->
            CategoryFilterChip(
                title = cat.titleAr,
                isSelected = selectedCategory == cat,
                onClick = { onCategorySelected(cat) }
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) NexusCyan.copy(alpha = 0.2f) else NexusSurface
    val borderColor = if (isSelected) NexusCyan else NexusCardBorder
    val textColor = if (isSelected) NexusCyan else NexusTextSecondary

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DangerFilterChips(
    selectedDanger: DangerLevel?,
    onDangerSelected: (DangerLevel?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DangerSelectChip(
            label = "كافة مستويات الخطر",
            color = NexusCyan,
            isSelected = selectedDanger == null,
            onClick = { onDangerSelected(null) }
        )
        DangerLevel.values().forEach { lvl ->
            DangerSelectChip(
                label = lvl.titleAr,
                color = Color(lvl.colorHex),
                isSelected = selectedDanger == lvl,
                onClick = { onDangerSelected(lvl) }
            )
        }
    }
}

@Composable
private fun DangerSelectChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) color.copy(alpha = 0.2f) else NexusSurface,
        border = BorderStroke(0.5.dp, if (isSelected) color else NexusCardBorder),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else NexusTextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DeepFeatureCard(
    feature: DeepDangerousFeature,
    onRunClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val dangerColor = Color(feature.dangerLevel.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deep_feature_card_${feature.number}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        border = BorderStroke(0.8.dp, dangerColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Number + Danger + Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Feature number badge
                    Surface(
                        color = Color(0xFF070B12),
                        border = BorderStroke(1.dp, dangerColor),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#${String.format("%03d", feature.number)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = dangerColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Danger Level
                    Surface(
                        color = dangerColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = feature.dangerLevel.badgeLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = dangerColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Category Tag
                Text(
                    text = feature.category.titleAr,
                    fontSize = 10.sp,
                    color = NexusTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Titles
            Text(
                text = feature.titleAr,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = NexusTextPrimary,
                lineHeight = 20.sp
            )
            Text(
                text = feature.titleEn,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = NexusTextSecondary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = feature.descriptionAr,
                fontSize = 12.sp,
                color = NexusTextPrimary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Explicit Warning Preview Banner (المطابقة لطلب المستخدم: تحذير قبل استخدامها)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = dangerColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, dangerColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = dangerColor,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "تحذير قبل الاستخدام:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = dangerColor
                        )
                        Text(
                            text = feature.warningAr,
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 15.sp,
                            maxLines = if (isExpanded) 10 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Expandable details (Subsystems, command preview)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "الأمر البرمجي التلقائي:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF04070D),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, NexusCardBorder)
                    ) {
                        Text(
                            text = feature.command,
                            modifier = Modifier.padding(8.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NexusCyan,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "الأنظمة المتأثرة: ${feature.affectedSubsystems.joinToString(", ")}",
                        fontSize = 10.sp,
                        color = NexusTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Expand / Collapse details toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "إخفاء التفاصيل" else "عرض التفاصيل والأمر",
                        fontSize = 11.sp,
                        color = NexusCyan,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Primary Run Button with Hazard Warning Indicator
                Button(
                    onClick = onRunClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dangerColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("run_deep_feature_button_${feature.number}")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تشغيل مع التحذير ⚠️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeepExecutionResultDialog(
    result: ExecutionResult,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (result.isSuccess) NexusGreen else NexusRed,
                    RoundedCornerShape(14.dp)
                ),
            color = Color(0xFF090E17)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    (if (result.isSuccess) NexusGreen else NexusRed).copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (result.isSuccess) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (result.isSuccess) NexusGreen else NexusRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (result.isSuccess) "تم تنفيذ التعديل بنجاح" else "فشل تنفيذ التعديل",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary
                        )
                    }

                    Text(
                        text = "Exit Code: ${result.exitCode}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NexusTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "الأمر المنفذ:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF03070D),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, NexusCardBorder)
                ) {
                    Text(
                        text = result.command,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NexusCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "مخرجات النظام (Output / Terminal):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF03070D),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, NexusCardBorder)
                ) {
                    val displayText = when {
                        result.stdout.isNotBlank() -> result.stdout
                        result.stderr.isNotBlank() -> result.stderr
                        result.isSuccess -> "Command executed with exit code 0. No stdout returned."
                        else -> "Command failed with exit code ${result.exitCode}"
                    }
                    Text(
                        text = displayText,
                        modifier = Modifier.padding(10.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (result.isSuccess) NexusTextPrimary else NexusRed,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(result.stdout.ifBlank { result.stderr }))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusSurfaceVariant,
                            contentColor = NexusTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "نسخ المخرجات", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusCyan,
                            contentColor = Color(0xFF001F28)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "إغلاق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
