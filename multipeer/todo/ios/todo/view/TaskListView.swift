//
//  TaskListView.swift
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

struct TaskListView: View {
    @ObservedObject var viewModel = TodoViewModel()
    @State private var newTask = ""
    @State private var showingPeers = false
    
    var body: some View {
        NavigationStack {
            VStack {
                TextField("Enter new task", text: $newTask)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .font(.system(size: 18))
                .padding()
                .background(Color(.systemGray6))
                .onSubmit {
                    guard !newTask.isEmpty else { return }
                    viewModel.addTask(name: newTask)
                    newTask = ""
                }
                
                List {
                    ForEach(viewModel.tasks) { task in
                        HStack {
                            Image(systemName: task.completed ? "checkmark.circle.fill" : "circle")
                                .onTapGesture {
                                    viewModel.toggleTask(task)
                                }
                            Text(task.name)
                                .strikethrough(task.completed)
                        }
                    }
                    .onDelete { indexSet in
                        indexSet.map {
                            viewModel.tasks[$0]
                        }.forEach(viewModel.deleteTask)
                    }
                }
                .listStyle(PlainListStyle())
            }
            .navigationTitle("Todo")
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack {
                        Text("\(viewModel.peerID)")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingPeers = true}) {
                        Image(systemName: "person.2")
                            .foregroundColor(viewModel.online ? .green : .gray)
                    }
                }
            }
            .sheet(isPresented: $showingPeers) {
                PeerListView(viewModel: viewModel)
            }
        }
    }
}

#Preview {
    TaskListView()
}
