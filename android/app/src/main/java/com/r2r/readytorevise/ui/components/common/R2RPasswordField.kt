package com.r2r.readytorevise.ui.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.r2r.readytorevise.ui.theme.AppIcons

@Composable
fun R2RPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {

    var visible by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,

        label = {
            Text(label)
        },

        singleLine = true,

        visualTransformation =
            if (visible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),

        trailingIcon = {

            IconButton(
                onClick = {
                    visible = !visible
                }
            ) {

                Icon(
                    imageVector =
                        if (visible)
                            AppIcons.ShowPassword
                        else
                            AppIcons.HidePassword,
                    contentDescription =
                        if (visible)
                            "Hide password"
                        else
                            "Show password"
                )

            }

        }

    )

}