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

class ColorSpecException : Exception {
    constructor(msg: String) : super(msg)
    constructor(msg: String, cause: Exception) : super(msg, cause)
}

data class Color(val red: Int, val green: Int, val blue: Int) {
    init {
        validateColor(red)
        validateColor(green)
        validateColor(blue)
    }

    override fun toString() = "#" + Integer.toHexString(red) + Integer.toHexString(green) + Integer.toHexString(blue)

    fun asInt() =  (0xff shl 24) or (red shl 16) or (green shl 8) or blue

    fun asRGBVector() = listOf(red, green, blue)

    companion object {
        @Throws(ColorSpecException::class)
        fun parse(colorCode: String): Color {
            if ((colorCode.length == 7) && (colorCode[0] == '#')) {
                try {
                    val color = colorCode.substring(1).toLong(16)
                    return Color(
                        ((color shr 16) and 0xffL).toInt(),
                        ((color shr 8) and 0xffL).toInt(),
                        (color and 0xffL).toInt()
                    )
                } catch (e: NumberFormatException) {
                    throw ColorSpecException("Unparsable color code " + e.message, e)
                }
            }

            val rgb = colorCode.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (rgb.size == 3) {
                return Color(parseColor(rgb[0]), parseColor(rgb[1]), parseColor(rgb[2]))
            }

            throw ColorSpecException("Unrecognized color format")
        }

        @Throws(ColorSpecException::class)
        private fun parseColor(colorCode: String): Int {
            try {
                return validateColor(colorCode.toInt())
            } catch (e: NumberFormatException) {
                throw ColorSpecException("Not a number: $colorCode", e)
            }
        }

        @Throws(ColorSpecException::class)
        private fun validateColor(c: Int): Int {
            if ((c < 0) || (c > 255)) {
                throw ColorSpecException("Color code must be a number between 0 and 255 inclusive: $c")
            }
            return c
        }
    }
}
