//
// Copyright (c) 2024 Couchbase, Inc All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.couchbase.lite.color.service


data class NamedColor(val id: String, val name: String, val distance: Int, val color: Color) {
    fun getColorCode() =
        "#${Integer.toHexString(color.red)}${Integer.toHexString(color.green)}${Integer.toHexString(color.blue)}"

    fun getRGBVector() = listOf(color.red.toString(), color.green.toString(), color.blue.toString())

    override fun toString() = "Color{${getColorCode()}(${id}): ${name} @${distance}}"
}