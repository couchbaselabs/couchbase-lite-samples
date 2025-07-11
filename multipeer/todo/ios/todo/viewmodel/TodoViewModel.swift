//
//  TodoViewModel.swift
//  todo
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

import SwiftUI
import CouchbaseLiteSwift

class TodoViewModel: ObservableObject {
    @Published var tasks: [Task] = []
    @Published var online = false
    @Published var peers: [Peer] = []
    @Published var peerID: String = ""
    
    private let databaseName = "todo"
    private let collectionName = "tasks"
    
    private let groupID = "com.couchbase.multipeer.todo"
    private let commonName = "multipeer-todo"
    private let identityLabel = "com.couchbase.multipeer.todo.identity"
    
    private let activities = ["stopped", "offline", "connecting", "idle", "busy"]
    
    private var db: Database!
    private var collection: Collection!
    private var replicator: MultipeerReplicator!
    private var updatePeersWorkItem: DispatchWorkItem?

    init() {
        setupLogging()
        setupDatabase()
        setupQuery()
        setupReplicator()
    }
    
    private func setupLogging() {
        LogSinks.console = ConsoleLogSink(level: .verbose, domains: [.multipeer, .peerDiscovery, .replicator])
    }

    private func setupDatabase() {
        do {
            db = try Database(name: databaseName)
            collection = try db.createCollection(name: collectionName)
        } catch {
            print("Setup Database Error: \(error)")
        }
    }

    private func setupReplicator() {
        do {
            let coll = MultipeerCollectionConfiguration(collection: collection)
            let identity = try peerIdentity()
            let auth = MultipeerCertificateAuthenticator { peer, certs in true }
            let config = MultipeerReplicatorConfiguration(
                peerGroupID: groupID,
                identity: identity,
                authenticator: auth,
                collections: [coll])
            replicator = try MultipeerReplicator(config: config)
            peerID = replicator.peerID.short
            
            _ = replicator.addStatusListener { status in
                self.online = status.active
                if let error = status.error {
                    print("Multipeer Replicator Error: \(error)")
                }
            }
            
            _ = replicator.addPeerDiscoveryStatusListener(listener: { status in
                self.updatePeers()
            })
            
            _ = replicator.addPeerReplicatorStatusListener { status in
                self.updatePeers(delay: 2.0)
            }
            
            replicator.start()
        } catch {
            print("Setup Replicator Error: \(error)")
        }
    }
    
    private func peerIdentity() throws -> TLSIdentity {
        let identity = try? TLSIdentity.identity(withLabel: identityLabel)

        if let identity, identity.expiration > Date() {
            return identity
        }
        
        if identity != nil {
            try? TLSIdentity.deleteIdentity(withLabel: identityLabel)
        }

        let attrs = [certAttrCommonName: commonName]
        return try TLSIdentity.createIdentity(
            for: [.clientAuth, .serverAuth],
            attributes: attrs,
            label: identityLabel)
    }
    
    func updatePeers(delay: TimeInterval = 0.0) {
        updatePeersWorkItem?.cancel()
        
        let workItem = DispatchWorkItem { [weak self] in
            self?.doUpdatePeers()
        }
        updatePeersWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: workItem)
    }
    
    private func doUpdatePeers() {
        var peers: [Peer] = []
        replicator.neighborPeers.forEach { peerID in
            if let peerInfo = replicator.peerInfo(for: peerID) {
                let online = peerInfo.online
                let activity = activities[Int(peerInfo.replicatorStatus.activity.rawValue)]
                var status = "\(activity)"
                if let error = peerInfo.replicatorStatus.error?.localizedDescription {
                    status += " - \(error)"
                }
                peers.append(Peer(peerID: peerID, online: online, status: status))
            }
        }
        self.peers = peers
    }

    private func setupQuery() {
        do {
            let sql = "SELECT meta().id AS id, name, completed FROM tasks"
            let query = try db.createQuery(sql)
            query.addChangeListener { change in
                var tasks: [Task] = []
                change.results?.forEach { result in
                    let id = result.string(at: 0)!
                    let name = result.string(at: 1)!
                    let completed = result.boolean(at: 2)
                    let task = Task(id: id, name: name, completed: completed)
                    tasks.append(task)
                }
                self.tasks = tasks
            }
        } catch {
            print("Setup Query Error: \(error)")
        }
    }

    func addTask(name: String) {
        let task = Task(id: UUID().uuidString, name: name, completed: false)
        save(task)
    }

    func toggleTask(_ task: Task) {
        var updated = task
        updated.completed.toggle()
        save(updated)
    }

    func deleteTask(_ task: Task) {
        do {
            if let doc = try collection.document(id: task.id) {
                try collection.delete(document: doc)
            }
        } catch {
            print("Delete error: \(error)")
        }
    }

    func save(_ task: Task) {
        do {
            let doc = try collection.document(id: task.id)?.toMutable() ?? MutableDocument(id: task.id)
            doc.setString(task.name, forKey: "name")
            doc.setBoolean(task.completed, forKey: "completed")
            try collection.save(document: doc)
        } catch {
            print("Save error: \(error)")
        }
    }
}

extension PeerID {
    var short: String {
        return String("\(self)".prefix(6))
    }
}
