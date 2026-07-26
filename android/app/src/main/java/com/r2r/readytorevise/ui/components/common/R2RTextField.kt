package com.r2r.readytorevise.ui.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun R2RTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {

    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),

        value = value,

        onValueChange = onValueChange,

        singleLine = true,

        label = {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f)
            )
        },

        textStyle = TextStyle(
            color = Color.White
        ),

        keyboardOptions = keyboardOptions,

        keyboardActions = keyboardActions,

        shape = RoundedCornerShape(15.dp),

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),

            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,

            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White.copy(alpha = 0.8f),

            cursorColor = Color.White,

            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}