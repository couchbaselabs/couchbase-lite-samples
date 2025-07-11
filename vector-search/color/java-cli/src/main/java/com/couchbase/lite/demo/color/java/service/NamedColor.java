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
import java.util.Objects;



public final class NamedColor {
    @NonNull
    private final Color color;
    @NonNull
    private final String id;
    @NonNull
    private final String name;
    private final int distance;

    public NamedColor(
        @NonNull final String id,
        @NonNull final String name,
        final int distance,
        @NonNull final Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.distance = distance;
    }

    @NonNull
    public String getId() { return id; }

    @NonNull
    public String getName() { return name; }

    public double getDistance() { return distance; }

    @NonNull
    public String getColorCode() {
        return "#"
            + Integer.toHexString(color.getRed())
            + Integer.toHexString(color.getGreen())
            + Integer.toHexString(color.getBlue());
    }

    @NonNull
    public List<Object> getRGBVector() {
        return Arrays.asList(
            Integer.toString(color.getRed()),
            Integer.toString(color.getGreen()),
            Integer.toString(color.getBlue()));
    }

    @Override
    @NonNull
    public String toString() {
        return "Color{@" + id + ", " + name + ", " + distance + ", " + getColorCode() + "}";
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = hash * 31 + name.hashCode();
        hash = hash * 31 + Double.hashCode(this.distance);
        return hash * 31 + color.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) { return true; }
        if (!(other instanceof NamedColor)) { return false; }
        final NamedColor c = (NamedColor) other;
        return Objects.equals(id, c.id)
            && Objects.equals(name, c.name)
            && Objects.equals(distance, c.distance)
            && Objects.equals(color, c.color);
    }
}

