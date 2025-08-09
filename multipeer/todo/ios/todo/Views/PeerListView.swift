//
//  PeerListView.swift
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

struct PeerListView: View {
    @ObservedObject var viewModel: TodoViewModel

    var body: some View {
        NavigationStack {
            VStack {
                List(viewModel.peers) { peer in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(peer.id.short())
                                .font(.headline)
                            Text(peer.status)
                                .font(.subheadline)
                                .foregroundColor(.gray)
                        }
                        Spacer()
                        Circle()
                            .fill(peer.online ? Color.green : Color.red)
                            .frame(width: 12, height: 12)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Peers")
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack {
                        Text("\(viewModel.myPeerID.short())")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                }
            }
        }
        .onAppear {
            viewModel.refreshPeers()
        }
    }
}

#Preview {
    PeerListView(viewModel: TodoViewModel())
}
