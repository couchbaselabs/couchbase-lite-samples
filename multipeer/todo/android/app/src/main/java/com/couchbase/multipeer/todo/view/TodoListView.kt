//
//  Copyright (c) 2025 Couchbase, Inc All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.couchbase.multipeer.todo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.couchbase.multipeer.todo.viewmodel.TodoViewModel
import com.couchbase.multipeer.todo.data.short
import com.couchbase.multipeer.todo.ui.theme.GreenOnline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListView(viewModel: TodoViewModel = viewModel()) {
    var newTask by remember { mutableStateOf("") }
    val tasks by viewModel.tasks.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    var showPeers by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Todo",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            viewModel.myPeerID.short,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                },
                actions = {
                    IconButton(onClick = { showPeers = true }) {
                        Icon(Icons.Default.People,
                            contentDescription = "Show Peers",
                            tint = if (isOnline) GreenOnline else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            TextField(
                value = newTask,
                onValueChange = { newTask = it },
                placeholder = { Text("Enter new task") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newTask.isNotBlank()) {
                            viewModel.addTask(newTask.trim())
                            newTask = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(tasks.size) { index ->
                    val task = tasks[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.completed,
                            onCheckedChange = {
                                viewModel.toggleTask(task.id)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = task.creator.short,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
    }

    if (showPeers) {
        PeerListView(
            viewModel = viewModel,
            onDismiss = { showPeers = false }
        )
    }
}