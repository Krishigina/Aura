package com.aura.feature.survey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.data.api.AuraApiClient
import com.aura.core.i18n.StringsRu
import com.aura.core.data.repository.TokenManager
import com.aura.core.data.repository.SkinPassportManager
import kotlinx.coroutines.launch

private val MeshBgColor = Color(0xFFE0F2F1)
private val DustyRose = Color(0xFFE8A5B8)
private val VibrantPink = Color(0xFFF472B6)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate300 = Color(0xFFCBD5E1)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald500 = Color(0xFF10B981)
private val Emerald600 = Color(0xFF059669)
private val Emerald700 = Color(0xFF047857)
private val Emerald800 = Color(0xFF065F46)

private data class SurveyQuestion(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val multiChoice: Boolean = false,
    val options: List<String>
)

private data class SurveySection(
    val title: String,
    val questions: List<SurveyQuestion>
)

private val surveySections = listOf(
    SurveySection(
        title = "1. БАЗОВЫЕ ХАРАКТЕРИСТИКИ КОЖИ",
        questions = listOf(
            SurveyQuestion("skin_type", "Как бы вы описали свою кожу?", options = listOf("Очень сухая", "Сухая", "Нормальная", "Комбинированная", "Жирная", "Очень жирная")),
            SurveyQuestion("after_wash", "Как кожа ведет себя через 2–3 часа после умывания?", options = listOf("Сильно стягивает", "Слегка сухая", "Комфортная", "Блеск в Т-зоне", "Блеск по всему лицу")),
            SurveyQuestion("pores", "Насколько выражены поры?", options = listOf("Почти незаметны", "Небольшие", "Заметны в Т-зоне", "Крупные по всему лицу")),
            SurveyQuestion("flaking", "Есть ли шелушения?", options = listOf("Никогда", "Иногда", "Часто", "Постоянно")),
            SurveyQuestion("age_group", "Возрастная группа", options = listOf("до 18", "18–24", "25–34", "35–44", "45+")),
            SurveyQuestion("phototype", "Фототип кожи", options = listOf("Очень светлая", "Светлая", "Средняя", "Смуглая", "Темная"))
        )
    ),
    SurveySection(
        title = "2. ТЕКУЩИЕ ПРОБЛЕМЫ",
        questions = listOf(
            SurveyQuestion(
                "skin_issues",
                "Какие проблемы кожи вас беспокоят?",
                subtitle = "Можно выбрать несколько",
                multiChoice = true,
                options = listOf(
                    "Акне", "Черные точки", "Расширенные поры", "Жирный блеск", "Сухость", "Обезвоженность",
                    "Пигментация", "Постакне", "Покраснения", "Купероз/сосуды", "Морщины", "Потеря упругости",
                    "Тусклый цвет", "Неровный рельеф", "Ничего не беспокоит"
                )
            )
        )
    ),
    SurveySection(
        title = "3. ЧУВСТВИТЕЛЬНОСТЬ И РЕАКЦИИ",
        questions = listOf(
            SurveyQuestion("new_products_reaction", "Как кожа реагирует на новые средства?", options = listOf("Никогда нет реакции", "Иногда легкое покраснение", "Часто раздражение", "Почти всегда реакция")),
            SurveyQuestion("allergy", "Бывают ли аллергические реакции?", options = listOf("Нет", "Редко", "Иногда", "Часто")),
            SurveyQuestion("triggers", "Как кожа реагирует на внешние факторы?", subtitle = "Можно выбрать несколько", multiChoice = true, options = listOf("Холод → покраснение", "Солнце → раздражение", "Ветер → сухость", "Косметика → жжение", "Ничего из перечисленного")),
            SurveyQuestion("diagnosis", "Есть ли диагнозы?", multiChoice = true, options = listOf("Акне", "Розацеа", "Дерматит", "Экзема", "Нет"))
        )
    ),
    SurveySection(
        title = "4. ОБРАЗ ЖИЗНИ",
        questions = listOf(
            SurveyQuestion("climate", "В каком климате вы живете?", options = listOf("Холодный", "Умеренный", "Жаркий", "Переменный")),
            SurveyQuestion("stress", "Уровень стресса", options = listOf("Низкий", "Средний", "Высокий", "Очень высокий")),
            SurveyQuestion("sleep", "Сон", options = listOf("7–8 часов", "5–7 часов", "<5 часов", "Нерегулярный")),
            SurveyQuestion("food", "Питание", options = listOf("Сбалансированное", "Есть сладкое/жирное", "Частый фастфуд", "Строгие диеты")),
            SurveyQuestion("water", "Вода", options = listOf("2 л/день", "1–2 л", "<1 л")),
            SurveyQuestion("smoking", "Курение", options = listOf("Нет", "Иногда", "Да")),
            SurveyQuestion("activity", "Физическая активность", options = listOf("Регулярная", "Умеренная", "Низкая")),
            SurveyQuestion("environment", "Где вы проводите больше времени?", options = listOf("Офис/помещение", "На улице", "Смешанный режим"))
        )
    ),
    SurveySection(
        title = "5. ИСТОРИЯ УХОДА",
        questions = listOf(
            SurveyQuestion("routine_level", "Используете ли уход?", options = listOf("Нет", "Минимальный", "Базовый", "Полный уход")),
            SurveyQuestion("used_products", "Какие средства используете?", subtitle = "Можно выбрать несколько", multiChoice = true, options = listOf("Очищение", "Тоник", "Крем", "Сыворотка", "SPF", "Кислоты", "Ретиноиды", "Маски")),
            SurveyQuestion("negative_reactions", "Были ли негативные реакции на", subtitle = "Можно выбрать несколько", multiChoice = true, options = listOf("Кислоты", "Ретиноиды", "Витамин C", "SPF", "Не было")),
            SurveyQuestion("actives_experience", "Использовали ли активы?", options = listOf("Никогда", "Иногда", "Регулярно"))
        )
    ),
    SurveySection(
        title = "6. ЦЕЛИ ПОЛЬЗОВАТЕЛЯ",
        questions = listOf(
            SurveyQuestion("goals", "Что вы хотите улучшить?", subtitle = "Можно выбрать несколько", multiChoice = true, options = listOf("Избавиться от акне", "Уменьшить жирность", "Увлажнить кожу", "Осветлить пигментацию", "Уменьшить морщины", "Выровнять тон", "Улучшить текстуру", "Поддерживать текущее состояние")),
            SurveyQuestion("priority", "Какой результат важнее?", options = listOf("Быстрый эффект", "Безопасность", "Долгосрочный результат")),
            SurveyQuestion("active_readiness", "Готовность к активным компонентам", options = listOf("Только мягкий уход", "Умеренные активы", "Готов к сильным активам")),
            SurveyQuestion("budget", "Бюджет на уход в месяц", options = listOf("До 3 000 ₽", "3 000–8 000 ₽", "8 000–15 000 ₽", "Без ограничений")),
            SurveyQuestion("preferred_format", "Какой формат рекомендаций удобнее?", options = listOf("Краткий план", "Подробный план", "Смешанный"))
        )
    )
)

@Composable
fun AuraSkinSurveyScreen(
    apiClient: AuraApiClient,
    allowSkip: Boolean,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var sectionIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, MutableSet<String>>() }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun applyAnswers(input: Map<String, List<String>>) {
        answers.clear()
        input.forEach { (key, value) ->
            answers[key] = value.toMutableSet()
        }
    }

    LaunchedEffect(Unit) {
        SkinPassportManager.passport?.let { applyAnswers(it.answers) }

        val token = TokenManager.getToken()
        if (!token.isNullOrBlank()) {
            runCatching { apiClient.getSkinPassport(token) }
                .getOrNull()
                ?.let { serverPassport ->
                    if (serverPassport.answers.isNotEmpty()) {
                        applyAnswers(serverPassport.answers)
                        SkinPassportManager.save(serverPassport.answers)
                    }
                }
        }
    }

    val currentSection = surveySections[sectionIndex]
    val progress = (sectionIndex + 1).toFloat() / surveySections.size

    Box(modifier = Modifier.fillMaxSize().background(MeshBgColor)) {
        SurveyMeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 140.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(currentSection.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate800)

            currentSection.questions.forEach { question ->
                SurveyQuestionCard(
                    question = question,
                    selected = answers[question.id] ?: mutableSetOf(),
                    onSelect = { option ->
                        val current = answers[question.id] ?: mutableSetOf()
                        if (question.multiChoice) {
                            if (current.contains(option)) current.remove(option) else current.add(option)
                        } else {
                            current.clear()
                            current.add(option)
                        }
                        answers[question.id] = current
                    }
                )
            }
        }

        SurveyHeader(
            sectionIndex = sectionIndex,
            sectionCount = surveySections.size,
            progress = progress,
            allowSkip = allowSkip,
            onSkip = onSkip,
            onBack = {
                if (sectionIndex == 0) onBack() else sectionIndex -= 1
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        SurveyFooter(
            isLast = sectionIndex == surveySections.lastIndex,
            isLoading = isSaving,
            modifier = Modifier.align(Alignment.BottomCenter),
            onContinue = {
                if (isSaving) return@SurveyFooter

                if (sectionIndex == surveySections.lastIndex) {
                    val normalizedAnswers = answers.mapValues { it.value.toList() }
                    SkinPassportManager.save(
                        answers = normalizedAnswers
                    )

                    val token = TokenManager.getToken()
                    if (token.isNullOrBlank()) {
                        onComplete()
                        return@SurveyFooter
                    }

                    isSaving = true
                    coroutineScope.launch {
                        runCatching {
                            apiClient.saveSkinPassport(
                                token = token,
                                answers = normalizedAnswers
                            )
                        }
                        isSaving = false
                        onComplete()
                    }
                } else {
                    sectionIndex += 1
                }
            }
        )
    }
}

@Composable
private fun SurveyMeshBackground() {
    Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
        val width = size.width
        val height = size.height
        drawCircle(color = Color(0xFFA7F3D0).copy(alpha = 0.5f), radius = width * 0.5f, center = Offset(0f, 0f))
        drawCircle(color = Color(0xFFE0C3FC).copy(alpha = 0.6f), radius = width * 0.5f, center = Offset(width, 0f))
        drawCircle(color = Color(0xFFA7F3D0).copy(alpha = 0.5f), radius = width * 0.5f, center = Offset(width, height))
        drawCircle(color = Color(0xFFBFDBFE).copy(alpha = 0.6f), radius = width * 0.5f, center = Offset(0f, height))
    }
}

@Composable
private fun SurveyHeader(
    sectionIndex: Int,
    sectionCount: Int,
    progress: Float,
    allowSkip: Boolean,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = StringsRu.Common.back, tint = Slate700)
                }
                Text(StringsRu.Survey.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate800)
                if (allowSkip) {
                    Text(
                        text = StringsRu.Common.skip,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSkip() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(StringsRu.Survey.progress, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald700, letterSpacing = 2.sp)
                    Text("${sectionIndex + 1} ИЗ $sectionCount ${StringsRu.Survey.blocksSuffix}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 2.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Emerald500))
                }
            }
        }
    }
}

@Composable
private fun SurveyQuestionCard(
    question: SurveyQuestion,
    selected: Set<String>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(question.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Slate800)
        if (question.subtitle.isNotBlank()) {
            Text(question.subtitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate500)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.forEachIndexed { index, option ->
                val selectedState = selected.contains(option)
                val icon = when (index % 8) {
                    0 -> Icons.Rounded.WaterDrop
                    1 -> Icons.Rounded.Face
                    2 -> Icons.Rounded.InvertColors
                    3 -> Icons.Rounded.Warning
                    4 -> Icons.Rounded.LocalHospital
                    5 -> Icons.Rounded.Verified
                    6 -> Icons.Rounded.HealthAndSafety
                    else -> Icons.Rounded.AutoAwesome
                }
                SurveyOptionRow(
                    title = option,
                    icon = icon,
                    isSelected = selectedState,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun SurveyOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .surveyGlassCard(isSelected)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (isSelected) Emerald500 else Emerald100.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Emerald600,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Emerald800 else Slate700,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (isSelected) Emerald500 else Color.Transparent, CircleShape)
                .border(
                    width = if (isSelected) 0.dp else 2.dp,
                    color = if (isSelected) Color.Transparent else Slate300,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
            }
        }
    }
}

@Composable
private fun SurveyFooter(
    isLast: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.5f)).blur(20.dp))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = VibrantPink.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(DustyRose, VibrantPink)))
                .clickable(enabled = !isLoading) { onContinue() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) StringsRu.Common.saving else if (isLast) StringsRu.Common.completeSurvey else StringsRu.Common.continueAction,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White)
        }
    }
}

private fun Modifier.surveyGlassCard(isSelected: Boolean = false): Modifier {
    val bgColor = if (isSelected) Emerald50.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.45f)
    val borderColor = if (isSelected) Color(0xFF6EE7B7).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
    return this
        .clip(RoundedCornerShape(24.dp))
        .background(bgColor)
        .border(1.dp, borderColor, RoundedCornerShape(24.dp))
}
