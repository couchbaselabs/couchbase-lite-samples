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

package com.couchbase.multipeer.todo.viewmodel

import androidx.lifecycle.ViewModel
import com.couchbase.multipeer.todo.data.DatabaseManager
import com.couchbase.multipeer.todo.model.Peer
import com.couchbase.multipeer.todo.model.Task
import kotlinx.coroutines.flow.StateFlow

class TodoViewModel(private val dbManager: DatabaseManager = DatabaseManager.instance) : ViewModel() {
    val tasks: StateFlow<List<Task>> = dbManager.tasks

    val peers: StateFlow<List<Peer>> = dbManager.peers

    val isOnline: StateFlow<Boolean> = dbManager.isOnline

    val myPeerID: String = dbManager.getMyPeerID()

    fun addTask(name: String) = dbManager.addTask(name)

    fun toggleTask(id: String) = dbManager.toggleTask(id)

    /** Not implemented in the UI yet */
    fun deleteTask(id: String) = dbManager.deleteTask(id)

    fun refreshPeers() = dbManager.refreshPeers()

    override fun onCleared() {
        super.onCleared()
        dbManager.close()
    }
}