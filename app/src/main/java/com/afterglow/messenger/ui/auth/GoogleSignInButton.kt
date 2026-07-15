package com.afterglow.messenger.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Text-only rather than Google's official branded button asset (this
// project has no way to bundle that logo file) -- swap in the real "G"
// mark later if you want pixel-perfect brand compliance.
@Composable
fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        if (enabled) {
            Text("Continue with Google")
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onSurface,
                strokeWidth = 2.dp
            )
        }
    }
}
