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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.couchbase.lite.Array;
import com.couchbase.lite.Collection;
import com.couchbase.lite.ConsoleLogger;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.FileLogger;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogFileConfiguration;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.Parameters;
import com.couchbase.lite.Query;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.VectorEncoding;
import com.couchbase.lite.VectorIndexConfiguration;
import com.couchbase.lite.demo.color.java.util.FileUtils;


public final class DBService {
    private Database db;
    private File workDir;

    @NonNull
    public DBService init() throws CouchbaseLiteException, IOException {
        Database db = this.db;
        if (db != null) { return this; }

        workDir = initCBL();
        CouchbaseLite.enableVectorSearch();
        initLogging(workDir);

        db = initDb(workDir);

        this.db = db;

        final Collection coll = db.getCollection("colors");
        if (coll == null) { throw new IllegalStateException("Collection 'colors' does not exist"); }

        final VectorIndexConfiguration config = new VectorIndexConfiguration("colorvect_l2", 3L, 2L);
        config.setEncoding(VectorEncoding.none());
        coll.createIndex("colors_index", config);

        return this;
    }

    public void close() throws CouchbaseLiteException {
        db.close();
        db = null;

        FileUtils.eraseFileOrDir(workDir);
    }

    @NonNull
    public List<NamedColor> search(@NonNull final Color color) throws CouchbaseLiteException {
        if (this.db == null) { throw new IllegalStateException("Database is not ready"); }

        final Query q = db.createQuery(
            "SELECT id, color, colorvect_l2, APPROX_VECTOR_DISTANCE(colorvect_l2, $vector) as dist" +
                " FROM colors" +
                " ORDER BY APPROX_VECTOR_DISTANCE(colorvect_l2, $vector)" +
                " LIMIT 8");

        final Parameters params = new Parameters();
        params.setArray("vector", new MutableArray(color.getRGBVector()));
        q.setParameters(params);

        final List<NamedColor> colors = new ArrayList<>();
        try (ResultSet rs = q.execute()) {
            for (Result result: rs.allResults()) {
                final String id = result.getString("id");
                if (id == null) { continue; }

                final String name = result.getString("color");
                if (name == null) { continue; }

                final int distance = result.getInt("dist");

                final Array rgb = result.getArray("colorvect_l2");
                if ((rgb == null) || (rgb.count() != 3)) { continue; }

                try {
                    colors.add(
                        new NamedColor(
                            id,
                            StringUtils.capitalize(name),
                            distance,
                            new Color(rgb.getInt(0), rgb.getInt(1), rgb.getInt(2))));
                }
                catch (Color.ColorSpecException ignore) { }
            }
        }

        return colors;
    }

    @NonNull
    private Database initDb(@NonNull final File workDir) throws IOException, CouchbaseLiteException {
        final File zipDir = new File(workDir, "zip");
        if (!zipDir.mkdirs()) { throw new IOException("Failed creating zip dir: " + zipDir); }

        try (InputStream res = getClass().getResourceAsStream("/db.cblite2.zip")) {
            if (res == null) { throw new IOException("DB resource not found"); }
            FileUtils.unzip(res, zipDir);
        }

        Database.copy(new File(workDir, "zip/db.cblite2"), "colors", new DatabaseConfiguration());
        FileUtils.eraseFileOrDir(zipDir);

        return new Database("colors");
    }

    @NonNull
    private File initCBL() throws IOException {
        final File curDir = Path.of("").toFile().getCanonicalFile();

        final File workDir = new File(curDir, ".colors");
        FileUtils.eraseFileOrDir(workDir);

        final File scratchDir = new File(workDir, "scratch");
        if (!scratchDir.mkdirs()) { throw new IOException("Failed creating scratch dir: " + scratchDir); }

        CouchbaseLite.init(false, workDir, scratchDir);

        return workDir;
    }

    private void initLogging(final File workDir) throws IOException {
        final ConsoleLogger console = Database.log.getConsole();
        console.setDomains(LogDomain.ALL_DOMAINS);
        console.setLevel(LogLevel.NONE);

        final File logDir = new File(workDir, "logs");
        if (!logDir.mkdirs()) { throw new IOException("Failed creating log dir: " + logDir); }
        final FileLogger logger = Database.log.getFile();
        logger.setConfig(new LogFileConfiguration(logDir.getCanonicalPath()));
        logger.setLevel(LogLevel.NONE);
    }
}
