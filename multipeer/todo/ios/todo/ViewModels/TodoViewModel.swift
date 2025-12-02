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

/// Manages todo tasks and multipeer connectivity status for the UI views.
@MainActor
class TodoViewModel: ObservableObject {
    @Published var tasks: [TodoTask] = []
    @Published var peers: [Peer] = []
    @Published var online: Bool = false
    
    let databaseManager = DatabaseManager.shared
    
    var myPeerID: String { databaseManager.myPeerID() }
    
    init() {
        do {
            try databaseManager.initialize()
            
            databaseManager.onTasksChange = { [weak self] tasks in
                self?.tasks = tasks
            }
            
            databaseManager.onPeersChange = { [weak self] peers in
                self?.peers = peers
            }
            
            databaseManager.onOnlineChange = { [weak self] online in
                self?.online = online
            }
        } catch {
            print("Failed to initialize DatabaseManager: \(error)")
        }
    }
    
    func addTask(name: String) {
        do {
            try databaseManager.addTask(name: name)
        } catch {
            print("Add task error: \(error)")
        }
    }
    
    func toggleTask(_ task: TodoTask) {
        do {
            try databaseManager.toggleTask(id: task.id)
        } catch {
            print("Toggle task error: \(error)")
        }
    }
    
    func deleteTask(_ task: TodoTask) {
        do {
            try databaseManager.deleteTask(id: task.id)
        } catch {
            print("Delete task error: \(error)")
        }
    }
    
    func updatePeers() {
        databaseManager.updatePeers()
    }
}
