package com.tgws.proxy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ChatMsg(val text: String, val isOutgoing: Boolean)

private val chatMessages = listOf(
    ChatMsg("Привет!", false),
    ChatMsg("Привет, как дела?", true),
    ChatMsg("Норм, ты как?", false),
    ChatMsg("Отлично, подключил прокси", true),
    ChatMsg("Работает стабильно?", false),
    ChatMsg("Да, летает 🔥", true),
    ChatMsg("Какой регион выбрал?", false),
    ChatMsg("Европа, пинг 30 мс", true),
    ChatMsg("Офигеть, круто", false),
    ChatMsg("Спасибо за помощь", false),
    ChatMsg("Всегда рад", true),
    ChatMsg("Кстати, новый сервер добавили", true),
    ChatMsg("Какой?", false),
    ChatMsg("Сингапур", true),
    ChatMsg("Понял, попробую", false),
    ChatMsg("Пиши если что", true),
    ChatMsg("Договорились 👍", false),
    ChatMsg("Удачи", true),
    ChatMsg("Пока", false),
    ChatMsg("Пока-пока", true),
    ChatMsg("Кстати, обнови приложение", true),
    ChatMsg("Уже скачиваю", false),
    ChatMsg("Там фичи крутые", true),
    ChatMsg("Вижу, интерфейс изменился", false),
    ChatMsg("Теперь все плавнее", true),
)

@Composable
fun ChatBackground(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    var listHeightPx by remember { mutableIntStateOf(0) }
    val offsetAnim = remember { Animatable(0f) }

    LaunchedEffect(listHeightPx, isActive) {
        if (!isActive || listHeightPx <= 0) {
            offsetAnim.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            offsetAnim.animateTo(
                targetValue = -listHeightPx.toFloat(),
                animationSpec = tween(35000, easing = LinearEasing)
            )
            offsetAnim.snapTo(0f)
        }
    }

    val baseAlpha = if (isActive) 0.55f else 0.25f

    Box(
        modifier = modifier
            .fillMaxSize()
            .blur(3.dp)
            .alpha(baseAlpha)
            .graphicsLayer(translationY = offsetAnim.value)
    ) {
        Column(
            modifier = Modifier.onGloballyPositioned { listHeightPx = it.size.height / 2 }
        ) {
            chatMessages.forEach { ChatBubble(it, isActive) }
            chatMessages.forEach { ChatBubble(it, isActive) }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMsg, isActive: Boolean) {
    val bubbleColor = when {
        !isActive && msg.isOutgoing -> Color(0xFF808080).copy(alpha = 0.55f)
        !isActive && !msg.isOutgoing -> Color(0xFFA0A0A0).copy(alpha = 0.45f)
        msg.isOutgoing -> AppColors.telegramBlue.copy(alpha = 0.80f)
        else -> Color.White.copy(alpha = 0.75f)
    }
    val textColor = when {
        !isActive -> Color(0xFF333333)
        msg.isOutgoing -> Color.White
        else -> Color(0xFF1A1A1A)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text = msg.text,
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 184.dp)
                .background(bubbleColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}
