package com.levinhocall.gitagent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(model: ChatViewModel = viewModel()) {
    val state by model.state.collectAsState()
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Git Agent", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            TextButton(onClick = model::clear) { Text("Clear") }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.messages) { message ->
                Text(if (message.role == "user") "You: ${message.text}" else "Assistant: ${message.text}", modifier = Modifier.fillMaxWidth())
            }
            if (state.isLoading) item { CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Assistant is thinking" }) }
            state.error?.let { error -> item { Text("Error: $error", color = androidx.compose.material3.MaterialTheme.colorScheme.error) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.draft, onValueChange = model::updateDraft, modifier = Modifier.weight(1f), label = { Text("Message") }, enabled = !state.isLoading)
            Button(onClick = model::send, enabled = state.draft.isNotBlank() && !state.isLoading, modifier = Modifier.semantics { contentDescription = "Send message" }) { Text("Send") }
        }
    }
}
