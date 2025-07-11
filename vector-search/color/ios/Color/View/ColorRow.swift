//
//  ColorRow.swift
//  Color
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

struct ColorRow: View {
    var color: ColorObject
    
    var body: some View {
        HStack {
            Text("")
                .frame(width: 38, height: 38)
                .background(Color(rgb: color.rgb))
            Text(color.id)
                .font(.headline)
            Text(color.name)
                .font(.subheadline)
            Spacer()
            Text(String(format: "%.0f", color.distance))
                .font(.subheadline)
                .foregroundStyle(.gray)
        }
    }
}

#Preview {
    ColorRow(color: ColorObject(id: "#FF0000", name: "Red", rgb: [255, 0, 0], distance: 1.0))
}
