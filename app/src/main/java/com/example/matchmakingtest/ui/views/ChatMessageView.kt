package com.example.matchmakingtest.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matchmakingtest.ui.models.MessageSentOrReceived
import com.example.matchmakingtest.ui.models.SingleMessage

@Composable
private fun SingleMessageView(message: SingleMessage) {
    val arrangementValue = when (message.isSentOrReceived) {
        MessageSentOrReceived.Received -> Arrangement.Start
        else -> Arrangement.End
    }
    val bgColor = when (message.isSentOrReceived) {
        MessageSentOrReceived.Received -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    val textColor: Color = when (message.isSentOrReceived) {
        MessageSentOrReceived.Received -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondary
    }
    val textAlignmentValue: TextAlign = when (message.isSentOrReceived) {
        MessageSentOrReceived.Received -> TextAlign.End
        else -> TextAlign.Start
    }

    val startPadding = when (message.isSentOrReceived) {
        MessageSentOrReceived.Received -> 16.dp
        else -> 32.dp
    }
    val endPadding = when (message.isSentOrReceived) {
        MessageSentOrReceived.Received -> 32.dp
        else -> 16.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, top = 8.dp, end = endPadding, bottom = 8.dp),
        horizontalArrangement = arrangementValue
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 16.sp,
                textAlign = textAlignmentValue
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewChatMessageView() {
    Column {
        SingleMessageView(
            message = SingleMessage(
                "Hello, how are you?",
                MessageSentOrReceived.Received
            )
        )
        SingleMessageView(
            message = SingleMessage(
                "I'm good, thank you!",
                MessageSentOrReceived.Sent
            )
        )
        SingleMessageView(
            message = SingleMessage(
                "What are you up to?",
                MessageSentOrReceived.Received
            )
        )
        SingleMessageView(
            message = SingleMessage(
                "Just working on a project.",
                MessageSentOrReceived.Sent
            )
        )
        SingleMessageView(
            message = SingleMessage(
                "Sounds interesting!",
                MessageSentOrReceived.Received
            )
        )
    }
}

@Composable
fun ChatMessagesView(modifier: Modifier, messages: List<SingleMessage>) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = messages.size,
            key = { i -> messages[i].hashCode().toString() + "$i" },
            itemContent = { SingleMessageView(message = messages[it]) }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewChatMessagesView() {
    val messages = listOf(
        SingleMessage("Hello, how are you?", MessageSentOrReceived.Received),
        SingleMessage("I'm good, thank you!", MessageSentOrReceived.Sent),
        SingleMessage("What are you up to?", MessageSentOrReceived.Received),
        SingleMessage("Just working on a project.", MessageSentOrReceived.Sent),
        SingleMessage("Sounds interesting!", MessageSentOrReceived.Received)
    )
    ChatMessagesView(modifier = Modifier, messages = messages)
}