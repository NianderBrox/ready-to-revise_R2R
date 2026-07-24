package com.r2r.readytorevise.ui.components.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.r2r.readytorevise.ui.theme.AppDimensions

@Composable
fun R2RLoading(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier.size(AppDimensions.SmallLoaderSize),
        strokeWidth = AppDimensions.LoaderStrokeWidth
    )
}