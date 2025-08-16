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

package com.couchbase.multipeer.todo.data

import android.content.Context
import android.util.Log
import com.couchbase.lite.Collection
import com.couchbase.lite.CouchbaseLite
import com.couchbase.lite.CouchbaseLiteException
import com.couchbase.lite.Database
import com.couchbase.lite.KeyUsage
import com.couchbase.lite.ListenerToken
import com.couchbase.lite.LogDomain
import com.couchbase.lite.LogLevel
import com.couchbase.lite.MultipeerCertificateAuthenticator
import com.couchbase.lite.MultipeerCollectionConfiguration
import com.couchbase.lite.MultipeerReplicator
import com.couchbase.lite.MultipeerReplicatorConfiguration
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.PeerInfo.PeerId
import com.couchbase.lite.TLSIdentity
import com.couchbase.lite.internal.BaseTLSIdentity.CERT_ATTRIBUTE_COMMON_NAME
import com.couchbase.lite.logging.ConsoleLogSink
import com.couchbase.lite.logging.LogSinks
import com.couchbase.multipeer.todo.model.Peer
import com.couchbase.multipeer.todo.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.EnumSet
import java.util.UUID

class DatabaseManager private constructor() {
    private lateinit var database: Database
    private lateinit var collection: Collection
    private lateinit var replicator: MultipeerReplicator

    private var queryListenerToken: ListenerToken? = null
    private var replicatorStatusToken: ListenerToken? = null

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshPeersJob: Job? = null

    companion object {
        private val TAG = "Todo"

        private val DATABASE_NAME = "todo"
        private val COLLECTION_NAME = "tasks"

        private val GROUP_ID = "com.couchbase.multipeer.todo"
        private val IDENTITY_LABEL = "com.couchbase.multipeer.todo.identity"
        private val IDENTITY_CERT_CN = "multipeer-todo"

        private val activities = listOf("stopped", "offline", "connecting", "idle", "busy")

        val instance: DatabaseManager by lazy { DatabaseManager() }
    }

    fun init(context: Context) {
        if (this::database.isInitialized) { return }
        CouchbaseLite.init(context)
        setupLog()
        setupDatabase()
        setupQuery()
        setupReplicator()
    }

    private fun setupLog() {
        LogSinks.get().console = ConsoleLogSink(LogLevel.VERBOSE,
            LogDomain.MULTIPEER, LogDomain.PEER_DISCOVERY, LogDomain.REPLICATOR)
    }

    private fun setupDatabase() {
        database = Database(DATABASE_NAME)
        collection = database.createCollection(COLLECTION_NAME)
    }

    private fun setupQuery() {
        try {
            val sql =
                "SELECT meta().id AS id, name, completed, creator, createdAt " +
                "FROM $COLLECTION_NAME " +
                "ORDER BY createdAt"
            val query = database.createQuery(sql)
            queryListenerToken = query.addChangeListener { change ->
                val tasks = change.results?.map { result ->
                    val id = result.getString("id")!!
                    val name = result.getString("name")!!
                    val completed = result.getBoolean("completed")
                    val creator = result.getString("creator")!!
                    val createdAt = result.getDate("createdAt")!!
                    Task(id, name, completed, creator, createdAt)
                } ?: emptyList()

                _tasks.value = tasks
            }
        } catch (e: CouchbaseLiteException) {
            Log.e(TAG, "Failed to create query: ${e.message}", e)
            _tasks.value = emptyList() // or show an error state
        }
    }

    private fun setupReplicator() {
        try {
            val collections = MultipeerCollectionConfiguration.fromCollections(listOf(collection))
            val identity = getPeerIdentity()
            val authenticator = MultipeerCertificateAuthenticator { _, _ -> true }

            val config = MultipeerReplicatorConfiguration.Builder()
                .setPeerGroupID(GROUP_ID)
                .setCollections(collections)
                .setIdentity(identity)
                .setAuthenticator(authenticator)
                .build()

            replicator = MultipeerReplicator(config)

            replicatorStatusToken = replicator.addStatusListener { status ->
                _isOnline.value = status.isActive
                if (status.error != null) {
                    Log.e(TAG, "Multipeer Replicator Error: ${status.error!!.message}")
                }
            }

            replicator.addPeerDiscoveryStatusListener { status ->
                refreshPeers()
            }

            replicator.addPeerReplicatorStatusListener { status ->
                refreshPeers()
            }

            replicator.start()
        }  catch (e: CouchbaseLiteException) {
            Log.e(TAG, "Failed to setup replicator: ${e.message}", e)
        }
    }

    private fun getPeerIdentity() : TLSIdentity {
        var identity = TLSIdentity.getIdentity(IDENTITY_LABEL)

        if (identity?.expiration?.before(Date()) == true) {
            // TLSIdentity.deleteIdentity(IDENTITY_LABEL)
            identity = null
        }

        if (identity == null) {
            val keyUsages: Set<KeyUsage> = EnumSet.of(
                KeyUsage.CLIENT_AUTH,
                KeyUsage.SERVER_AUTH
            )

            val cn = "$IDENTITY_CERT_CN-${UUID.randomUUID().toString().take(8)}"
            val attributes = mapOf(
                CERT_ATTRIBUTE_COMMON_NAME to cn
            )

            identity = TLSIdentity.createIdentity(
                keyUsages,
                attributes,
                null, /* Default : 1 Year */
                IDENTITY_LABEL
            )
        }

        return identity!!
    }

    fun getMyPeerID(): String {
        return replicator.peerId.str
    }

    fun addTask(name: String) {
        try {
            val doc = MutableDocument()
            doc.setString("name", name)
            doc.setBoolean("completed", false)
            doc.setString("creator", replicator.peerId.str)
            doc.setDate("createdAt", Date())
            collection.save(doc)
        } catch (e: CouchbaseLiteException) {
            Log.e(TAG, "Failed to add a new task: ${e.message}", e)
        }
    }

    fun toggleTask(id: String) {
        try {
            val doc = collection.getDocument(id)?.toMutable() ?: return
            val completed = doc.getBoolean("completed")
            doc.setBoolean("completed", !completed)
            collection.save(doc)
        } catch (e: CouchbaseLiteException) {
            Log.e(TAG, "Failed to toggle task '${id}': ${e.message}", e)
        }
    }

    fun deleteTask(id: String) {
        try {
            val doc = collection.getDocument(id) ?: return
            collection.delete(doc)
        } catch (e: CouchbaseLiteException) {
            Log.e(TAG, "Failed to delete task '${id}': ${e.message}", e)
        }
    }

    fun refreshPeers() {
        if (refreshPeersJob?.isActive == true) {
            return
        }
        refreshPeersJob = scope.launch {
            delay(2000)  // 2-second debounce like your Swift code
            doRefreshPeers()
        }
    }

    private fun doRefreshPeers() {
        val peers = replicator.neighborPeers.mapNotNull { id ->
            val info = replicator.getPeerInfo(id) ?: return@mapNotNull null
            var status = activities[info.replicatorStatus.activityLevel.ordinal]
            info.replicatorStatus.error?.let { status += " - ${it.localizedMessage}" }
            Peer(id = id.str, online = info.isOnline, status = status)
        }
        _peers.value = peers
    }

    fun close() {
        queryListenerToken?.remove()
        replicatorStatusToken?.remove()
        database.close()
    }
}

// Extensions:

val PeerId.str: String
    get() = toString()

val String.short: String
    get() = this.take(6)