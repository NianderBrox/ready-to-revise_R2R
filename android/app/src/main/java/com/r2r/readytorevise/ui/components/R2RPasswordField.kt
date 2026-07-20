package com.r2r.readytorevise.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    label: String
) {

    var visible by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
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