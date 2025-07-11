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
package com.couchbase.lite.demo.color.java;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.demo.color.java.service.Color;
import com.couchbase.lite.demo.color.java.service.DBService;
import com.couchbase.lite.demo.color.java.service.NamedColor;


public class UI {
    private final DBService svc;

    public UI(@NonNull final DBService svc) { this.svc = svc; }

    public void run() {
        banner();

        try (Scanner in = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("Enter color (#ffffff or 255 255 255)> ");
                final String targetColor = in.nextLine();
                System.out.println();
                if (targetColor.isEmpty()) { return; }

                try {
                    final List<NamedColor> colors = svc.search(Color.parse(targetColor));
                    System.out.println("Found " + colors.size() + " matches:");
                    for (NamedColor c: colors) {
                        System.out.println(String.format(
                            " *  Color %s (%s) @ %s",
                            c.getName(),
                            c.getId(),
                            c.getDistance()));
                    }
                }
                catch (CouchbaseLiteException e) {
                    throw new RuntimeException("Couchbase failure", e);
                }
                catch (Color.ColorSpecException e) {
                    System.out.println("Can't parse color: " + targetColor);
                    System.out.println("    " + e.getMessage());
                }
                System.out.println();
                System.out.println();
            }
        }
    }

    private void banner() {
        System.out.println();
        System.out.println();
        System.out.println("+=======================================+");
        System.out.println("|   A C M E   C O L O R   F I N D E R   |");
        System.out.println("+=======================================+");
    }
}
