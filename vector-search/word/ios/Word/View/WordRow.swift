//
//  WordRow.swift
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

struct WordRow: View {
    var word: Word
    
    var body: some View {
        HStack {
            Text(word.word)
                .font(.subheadline)
            Spacer()
            Text(String(format: "%.2f", word.distance))
                .font(.subheadline)
                .foregroundStyle(.gray)
        }
    }
}

#Preview {
    WordRow(word: Word(id: "word1", word: "dinner", distance: 1.0))
}
