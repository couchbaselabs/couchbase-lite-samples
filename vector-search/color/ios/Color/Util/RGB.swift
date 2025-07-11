//
//  RGB.swift
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

import Foundation

/// Generate RGB vectors from hex color codes or 3 space-separated numbers
class RGB {
    static func vector(for colorCode: String) throws -> [Int] {
        let code = colorCode.trimmingCharacters(in: .whitespaces)
        var vector: [Int] = [];
        if (code.count == 6) {  // Hex color code
            if let hex = Int(code, radix: 16) {
                vector.append(Int((hex >> 16) & 0xFF))
                vector.append(Int((hex >> 8)  & 0xFF))
                vector.append(Int((hex)       & 0xFF))
            }
        } else {                // 3 numbers
            let comps = (code as NSString).components(separatedBy: " ");
            if (comps.count == 3) {
                for comp in comps {
                    if let c = Int(comp), (c >= 0 && c <= 255) {
                        vector.append(Int(c))
                    } else {
                        break
                    }
                }
            }
        }
        if (vector.count != 3) {
            throw "Invalid Color Code"
        }
        return vector
    }
}
