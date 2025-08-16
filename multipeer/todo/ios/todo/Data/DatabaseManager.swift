//
//  DatabaseManager.swift
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

import Combine
import CouchbaseLiteSwift

/// DatabaseManager handles database access, queries, and multipeer replication.
///
/// This class is **not thread-safe** because it is intended to be used only from
/// the `TodoViewModel`, which is marked as a MainActor to ensure main-thread confinement.
/// Therefore, for simplicity, no internal synchronization is implemented.
final class DatabaseManager {
    /// Singleton instance
    static let shared = DatabaseManager()
    
    // MARK: - Constants
    private let databaseName = "todo"
    private let collectionName = "tasks"
    private let groupID = "com.couchbase.multipeer.todo"
    private let baseCommonName = "multipeer-todo"
    private let identityLabel = "com.couchbase.multipeer.todo.identity"
    private let activities = ["stopped", "offline", "connecting", "idle", "busy"]
    
    // MARK: - Database
    private var db: Database!
    private var collection: Collection!
    
    // MARK: - Query
    private var query: Query!
    
    // MARK: - MultipeerReplicator
    private var replicator: MultipeerReplicator!
    private var refreshPeersWorkItem: DispatchWorkItem?
    
    // MARK: - Publishers
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - State
    private var isInitialized = false
    
    // MARK: - Callbacks
    var onTasksChange: (([TodoTask]) -> Void)?
    var onPeersChange: (([Peer]) -> Void)?
    var onOnlineChange: ((Bool) -> Void)?
    
    // MARK: - Private Init
    
    /// Use DatabaseManager.shared for getting the instance.
    private init() { }
    
    // MARK: - Setup
    
    /// Need to be called first before anything else.
    func initialize() throws {
        guard !isInitialized else { return }
        
        LogSinks.console = ConsoleLogSink(
            level: .verbose,
            domains: [.multipeer, .peerDiscovery, .replicator]
        )
        
        try setupDatabase()
        try setupLiveQuery()
        try setupReplicator()
        
        isInitialized = true
    }
    
    private func setupDatabase() throws {
        db = try Database(name: databaseName)
        collection = try db.createCollection(name: collectionName)
    }
    
    private func setupLiveQuery() throws {
        let sql = """
            SELECT meta().id AS id, name, completed, creator, createdAt
            FROM \(collectionName)
            ORDER BY createdAt
        """
        query = try db.createQuery(sql)
        query.changePublisher()
            .sink { [weak self] change in
                guard let results = change.results else { return }
                let tasks: [TodoTask] = results.map { result in
                    return TodoTask(
                        id: result.string(at: 0)!,
                        name: result.string(at: 1)!,
                        completed: result.boolean(at: 2),
                        creator: result.string(at: 3)!,
                        createdAt: result.date(at: 4)!
                    )
                }
                self?.onTasksChange?(tasks)
            }
            .store(in: &cancellables)
    }
    
    private func setupReplicator() throws {
        let collections = MultipeerCollectionConfiguration.fromCollections([collection])
        let identity = try myPeerIdentity()
        let auth = MultipeerCertificateAuthenticator { _, _ in true }
        
        let config = MultipeerReplicatorConfiguration(
            peerGroupID: groupID,
            identity: identity,
            authenticator: auth,
            collections: collections
        )
        
        replicator = try MultipeerReplicator(config: config)
        
        replicator.statusPublisher()
            .sink { [weak self] status in
                if let error = status.error {
                    print("Multipeer Replicator Error: \(error)")
                }
                self?.onOnlineChange?(status.active)
            }
            .store(in: &cancellables)
        
        replicator.peerDiscoveryStatusPublisher()
            .sink { [weak self] status in
                self?.refreshPeers()
            }
            .store(in: &cancellables)
        
        replicator.peerReplicatorStatusPublisher()
            .sink { [weak self] status in
                self?.refreshPeers()
            }
            .store(in: &cancellables)
        
        replicator.start()
    }
    
    private func myPeerIdentity() throws -> TLSIdentity {
        if let identity = try TLSIdentity.identity(withLabel: identityLabel) {
            if identity.expiration > Date() {
                return identity
            }
            try? TLSIdentity.deleteIdentity(withLabel: identityLabel)
        }
        
        let cn = "\(baseCommonName)-\(UUID().uuidString.short(8))"
        
        return try TLSIdentity.createIdentity(
            for: [.clientAuth, .serverAuth],
            attributes: [certAttrCommonName: cn],
            label: identityLabel
        )
    }
    
    // MARK: - Tasks
    
    func addTask(name: String) throws {
        let doc = MutableDocument()
        doc.setString(name, forKey: "name")
        doc.setBoolean(false, forKey: "completed")
        doc.setString(myPeerID(), forKey: "creator")
        doc.setDate(Date(), forKey: "createdAt")
        try collection.save(document: doc)
    }
    
    func toggleTask(id: String) throws {
        guard let doc = try collection.document(id: id)?.toMutable() else { return }
        doc.setBoolean(!doc.boolean(forKey: "completed"), forKey: "completed")
        try collection.save(document: doc)
    }
    
    func deleteTask(id: String) throws {
        guard let doc = try collection.document(id: id) else { return }
        try collection.delete(document: doc)
    }
    
    // MARK: - Peers
    
    func myPeerID() -> String {
        replicator.peerID.str
    }
    
    func refreshPeers() {
        // If a refresh is already scheduled, ignore new calls
        guard refreshPeersWorkItem == nil else { return }
        
        let workItem = DispatchWorkItem { [weak self] in
            self?.doRefreshPeers()
            self?.refreshPeersWorkItem = nil
        }
        
        refreshPeersWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0, execute: workItem)
    }
    
    private func doRefreshPeers() {
        let peers = replicator.neighborPeers.compactMap { id -> Peer? in
            guard let info = replicator.peerInfo(for: id) else { return nil }
            
            var status = activities[Int(info.replicatorStatus.activity.rawValue)]
            if let error = info.replicatorStatus.error?.localizedDescription {
                status += " - \(error)"
            }
            
            return Peer(id: id.str, online: info.online, status: status)
        }
        onPeersChange?(peers)
    }
}
