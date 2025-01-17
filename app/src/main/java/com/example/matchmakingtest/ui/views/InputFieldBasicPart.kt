package com.example.matchmakingtest.ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun InputFieldBasicPart(
    textNow: String,
    onTextChanged: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textNow,
            onValueChange = onTextChanged,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                fontWeight = MaterialTheme.typography.titleLarge.fontWeight,
                textAlign = TextAlign.Start
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            maxLines = 1,
        )
        if (textNow.isEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "write something",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                fontWeight = MaterialTheme.typography.titleLarge.fontWeight,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}