//
//  ContentView.swift
//  Word
//
//  Copyright (c) 2024 Couchbase, Inc All rights reserved.
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

struct ContentView: View {
    @EnvironmentObject private var model: ContentViewModel
    
    @State var searchWord = ""
    
    @State private var displayError = false
    @State private var errorMessage = ""
    
    @FocusState private var isFocused :Bool
    
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading) {
                HStack {
                    HStack {
                        TextField("Enter a word about food (e.g. dinner)", text: $searchWord)
                            .frame(height: 44).padding(.horizontal, 20)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .focused($isFocused)
                            .onSubmit {
                                if (searchWord.count > 0) {
                                    isFocused = false
                                    search()
                                }
                            }
                    }
                    .background(Color.white)
                }
                .background(Color.white)
                
                List(model.words) { word in
                    WordRow(word: word)
                }
                .listStyle(.plain)
                Spacer()
            }
            .navigationTitle("Word")
            .alert("Error", isPresented: $displayError) {
                Button("OK", role: .cancel, action: {})
            } message: {
                Text(errorMessage)
            }
        }
        .task {
            do {
                try await model.initialize()
            } catch {
                displayError(error)
            }
        }
    }
    
    func search() {
        Task {
            do {
                try await model.search(searchWord)
            } catch {
                displayError(error)
            }
        }
    }
    
    func displayError(_ error: Error) {
        errorMessage = "\(error)"
        displayError = true
    }
}

#Preview {
    let model = ContentViewModel(AppService())
    model.words = [
        Word(id: "word1", word: "dinner", distance: 0.1)
    ]
    return ContentView(searchWord: "meal").environmentObject(model)
}
