package com.statushd.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.statushd.app.R
import com.statushd.app.config.AppConfig
import com.statushd.app.ui.components.StatusHdSecondaryButton
import com.statushd.app.ui.components.StatusHdTopBar
import com.statushd.app.ui.theme.ShSurface
import com.statushd.app.ui.theme.ShTextMuted

/** S14 — Bantuan: FAQ dahulu (akordion), kemudian e-mel sokongan. */
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val faqs = listOf(
        R.string.support_faq1_q to R.string.support_faq1_a,
        R.string.support_faq2_q to R.string.support_faq2_a,
        R.string.support_faq3_q to R.string.support_faq3_a,
        R.string.support_faq4_q to R.string.support_faq4_a,
    )
    Column(modifier = Modifier.fillMaxSize()) {
        StatusHdTopBar(
            title = stringResource(R.string.support_title),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            faqs.forEach { (q, a) ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { expanded = !expanded },
                    colors = CardDefaults.cardColors(containerColor = ShSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(q),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = null,
                            )
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(a),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ShTextMuted,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            StatusHdSecondaryButton(
                text = stringResource(R.string.support_email),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${AppConfig.SUPPORT_EMAIL}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
