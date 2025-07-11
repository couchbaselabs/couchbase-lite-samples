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
package com.couchbase.lite.demo.color.java.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public final class FileUtils {
    private FileUtils() { }

    public static boolean eraseFileOrDir(@NonNull final String fileOrDirectory) {
        return eraseFileOrDir(new File(fileOrDirectory));
    }

    public static boolean eraseFileOrDir(@NonNull final File fileOrDirectory) {
        return deleteRecursive(fileOrDirectory);
    }

    public static boolean deleteContents(@Nullable final String fileOrDirectory) {
        return deleteContents((fileOrDirectory == null) ? null : new File(fileOrDirectory));
    }

    public static boolean deleteContents(@Nullable final File fileOrDirectory) {
        if ((fileOrDirectory == null) || (!fileOrDirectory.isDirectory())) { return true; }

        final File[] contents = fileOrDirectory.listFiles();
        if (contents == null) { return true; }

        boolean succeeded = true;
        for (File file: contents) {
            if (!deleteRecursive(file)) { succeeded = false; }
        }

        return succeeded;
    }

    public static void unzip(@NonNull final InputStream src, @NonNull final File dst) throws IOException {
        final byte[] buffer = new byte[1024];
        try (InputStream in = src; ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                final File newFile = new File(dst, ze.getName());
                if (ze.isDirectory()) { newFile.mkdirs(); }
                else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) { fos.write(buffer, 0, len); }
                    }
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    private static boolean deleteRecursive(@NonNull final File fileOrDirectory) {
        return (!fileOrDirectory.exists()) || (deleteContents(fileOrDirectory) && fileOrDirectory.delete());
    }
}
