package com.levinhocall.gitagent

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levinhocall.gitagent.ai.AiProviderId
import com.levinhocall.gitagent.ai.AiRole

@Composable
fun ChatScreen(model: ChatViewModel) {
    val state by model.state.collectAsState()
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF05070E), Color(0xFF0C1120), Color(0xFF141A2F))
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AURIX", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFBFD3FF), fontWeight = FontWeight.Bold)
                Text(state.providerStatus, style = MaterialTheme.typography.bodySmall, color = Color(0xFF90A4D4))
            }
            TextButton(onClick = model::clearConversation, modifier = Modifier.semantics { contentDescription = "Clear conversation" }) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        AiCoreOrb(isLoading = state.isLoading)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                Button(
                    onClick = { providerExpanded = true },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Select provider" }
                ) {
                    Text(state.selectedProvider.name)
                }
                DropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                    state.availableProviders.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.name) },
                            onClick = {
                                model.selectProvider(provider)
                                providerExpanded = false
                            }
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                Button(
                    onClick = { modelExpanded = true },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Select model" }
                ) {
                    Text(state.selectedModel.ifBlank { "No model" })
                }
                DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                    state.availableModels.forEach { modelId ->
                        DropdownMenuItem(
                            text = { Text(modelId) },
                            onClick = {
                                model.selectModel(modelId)
                                modelExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.apiKeyDraft,
            onValueChange = model::updateApiKeyDraft,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Provider API key" },
            label = { Text("API key for ${state.selectedProvider.name}") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = model::saveApiKey, modifier = Modifier.semantics { contentDescription = "Save API key" }) { Text("Save Key") }
            TextButton(onClick = model::clearApiKey, modifier = Modifier.semantics { contentDescription = "Remove API key" }) { Text("Remove Key") }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFF29324A))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages) { message ->
                val isUser = message.role == AiRole.USER
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) Color(0xFF314A84) else Color(0xFF1D2438)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (if (isUser) "You" else "AURIX") + ": ${message.text}",
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFDCE7FF)
                    )
                }
            }
            if (state.isLoading) {
                item {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Assistant is thinking" })
                }
            }
            state.error?.let { error ->
                item { Text("Error: $error", color = MaterialTheme.colorScheme.error) }
            }
            if (state.memoryHint.isNotBlank()) {
                item { Text(state.memoryHint, color = Color(0xFF90A4D4), style = MaterialTheme.typography.bodySmall) }
            }
            item {
                AssistChip(onClick = {}, enabled = false, label = { Text(state.unsupportedStatus) })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = model::updateDraft,
                modifier = Modifier.weight(1f),
                label = { Text("Message AURIX") },
                enabled = !state.isLoading
            )
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.semantics { contentDescription = "Voice input not yet implemented" }
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF90A4D4))
            }
            IconButton(
                onClick = model::send,
                enabled = state.draft.isNotBlank() && !state.isLoading,
                modifier = Modifier.semantics { contentDescription = "Send message" }
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFFBFD3FF))
            }
        }

        ToolRow(state.availableProviders)
    }
}

@Composable
private fun AiCoreOrb(isLoading: Boolean) {
    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isLoading) 900 else 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((72 * scale).dp)
                .background(
                    brush = Brush.radialGradient(colors = listOf(Color(0xFF6D8BFF), Color(0xFF1A264A))),
                    shape = CircleShape
                )
                .semantics { contentDescription = "AURIX core" }
        )
    }
}

@Composable
private fun ToolRow(providers: List<AiProviderId>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        providers.forEach { provider ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("${provider.name}: tools pending") },
                modifier = Modifier.semantics { contentDescription = "Tool status ${provider.name}" }
            )
        }
    }
}
