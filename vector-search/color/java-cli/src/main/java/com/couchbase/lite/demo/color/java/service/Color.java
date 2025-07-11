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
package com.couchbase.lite.demo.color.java.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;


public class Color {
    public static class ColorSpecException extends Exception {
        public ColorSpecException(@NonNull String msg) { super(msg); }
        public ColorSpecException(@NonNull String msg, @NonNull Exception cause) { super(msg, cause); }
    }

    @NonNull
    public static Color parse(@NonNull final String colorCode) throws ColorSpecException {
        if ((colorCode.length() == 7) && (colorCode.charAt(0) == '#')) {
            try {
                final long color = Long.parseLong(colorCode.substring(1), 16);
                return new Color((int) ((color >> 16) & 0xff), (int) ((color >> 8) & 0xff), (int) (color & 0xff));
            }
            catch (NumberFormatException e) {
                throw new ColorSpecException("Unparsable color code " + e.getMessage(), e);
            }
        }

        final String[] rgb = colorCode.split(" ");
        if (rgb.length == 3) { return new Color(parseColor(rgb[0]), parseColor(rgb[1]), parseColor(rgb[2])); }

        throw new ColorSpecException("Unrecognized format");
    }

    private static int parseColor(@NonNull final String colorCode) throws ColorSpecException {
        try { return validColor(Integer.parseInt(colorCode)); }
        catch (NumberFormatException e) { throw new ColorSpecException("Not a number: " + colorCode, e); }
    }

    private static int validColor(int c) throws ColorSpecException {
        if ((c < 0) || (c > 255)) {
            throw new ColorSpecException("Color code must be a number between 0 and 255 inclusive: " + c);
        }
        return c;
    }

    private final int red;
    private final int green;
    private final int blue;

    public Color(final int red, final int green, final int blue) throws ColorSpecException {
        this.red = validColor(red);
        this.green = validColor(green);
        this.blue = validColor(blue);
    }

    public int getRed() { return red; }

    public int getGreen() { return green; }

    public int getBlue() { return blue; }

    @Override
    @NonNull
    public String toString() {
        return "#" + Integer.toHexString(red) + Integer.toHexString(green) + Integer.toHexString(blue);
    }

    @NonNull
    public List<Object> getRGBVector() {
        return Arrays.asList(Integer.valueOf(red), Integer.valueOf(green), Integer.valueOf(blue));
    }

    public int hashCode() { return 0x0ff000000 | (red << 16) | (green << 8) | blue; }

    public boolean equals(@Nullable Object other) {
        if (other == this) { return true; }
        if (!(other instanceof Color)) { return false; }
        final Color c = (Color) other;
        return c.hashCode() == hashCode();
    }
}
