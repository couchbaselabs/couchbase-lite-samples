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

package com.couchbase.lite.color.service

import android.content.Context
import com.couchbase.lite.Collection
import com.couchbase.lite.CouchbaseLite
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration
import com.couchbase.lite.LogDomain
import com.couchbase.lite.LogLevel
import com.couchbase.lite.MutableArray
import com.couchbase.lite.Parameters
import com.couchbase.lite.Query
import com.couchbase.lite.VectorEncoding
import com.couchbase.lite.VectorIndexConfiguration
import com.couchbase.lite.color.util.eraseFileOrDir
import com.couchbase.lite.color.util.mkPath
import com.couchbase.lite.color.util.unzip
import java.io.File
import java.io.IOException
import java.util.Locale


private const val DATABASE_NAME = "db"
private const val DATASET_FILE = "${DATABASE_NAME}.cblite2"
private const val DATASET_ZIP_FILE = "${DATASET_FILE}.zip"
private const val DATASET_UNZIP_DIR = "unzip"
private const val QUERY_EXPRESSION = "colorvect_l2"
private const val COLLECTION_NAME = "colors"
private const val INDEX_NAME = "colors_index"

/** App Service for searching similar colors from Couchbase Lite Database */
class AppService(private val context: Context) {
    private val lock = Any()

    private var database: Database? = null
    private var collection: Collection? = null
    private var query: Query? = null

    /**
     * Initialize the App's database by loading the dataset,
     * if it doesn't exist, and create a vector index and query.
     */
    fun start() = synchronized(lock) {
        if (query != null) {
            return
        }

        CouchbaseLite.init(context)
        CouchbaseLite.enableVectorSearch()
        initLogging()

        val dbConfig = DatabaseConfiguration()

        if (!Database.exists(DATABASE_NAME, File(dbConfig.directory))) {
            loadDataset(dbConfig)
        }

        val db = Database(DATABASE_NAME, dbConfig)
        database = db

        val coll = db.getCollection(COLLECTION_NAME)
            ?: throw IllegalStateException("Collection not found: ${COLLECTION_NAME}")
        collection = coll

        // Create vector index:
        val idxConfig = VectorIndexConfiguration(QUERY_EXPRESSION, 3L, 2L)
        idxConfig.encoding = VectorEncoding.none()
        coll.createIndex(INDEX_NAME, idxConfig)

        query = db.createQuery(
            """
            SELECT id, color, $QUERY_EXPRESSION, APPROX_VECTOR_DISTANCE($QUERY_EXPRESSION, ${'$'}vector) as dist
                FROM $COLLECTION_NAME
                ORDER BY APPROX_VECTOR_DISTANCE($QUERY_EXPRESSION, ${'$'}vector)
                LIMIT 8
            """.trimIndent()
        )
    }

    fun stop() = synchronized(lock) {
        query = null

        collection?.close()
        collection = null

        database?.close()
        database = null
    }

    /** Search similar colors. */
    fun search(color: Color): List<NamedColor> = synchronized(lock) {
        val q = query ?: throw IllegalStateException("Database not initialized")

        // Set $vector parameter on the query:
        val params = Parameters()
        params.setArray("vector", MutableArray(color.asRGBVector()))
        q.parameters = params

        // Execute search and return result:
        val colors = mutableListOf<NamedColor>()
        q.execute().use { rs ->
            for (result in rs.allResults()) {
                val id = result.getString("id") ?: continue
                val name = result.getString("color") ?: continue
                val distance = result.getInt("dist")
                val rgb = result.getArray(2) ?: continue
                if (rgb.count() != 3) {
                    continue
                }

                colors.add(
                    NamedColor(
                        id,
                        name.replaceFirstChar {
                            if (it.isUpperCase()) it.toString() else it.titlecase(Locale.getDefault())
                        },
                        distance,
                        Color(rgb.getInt(0), rgb.getInt(1), rgb.getInt(2))
                    )
                )
            }
        }

        return colors
    }

    /** Load dataset. */
    private fun loadDataset(config: DatabaseConfiguration) {
        val zipDir = File(config.directory, DATASET_UNZIP_DIR)
        if (zipDir.exists()) {
            zipDir.eraseFileOrDir()
        }
        zipDir.mkPath()

        context.assets.open(DATASET_ZIP_FILE).unzip(zipDir)

        Database.copy(File(zipDir, DATASET_FILE), DATABASE_NAME, DatabaseConfiguration())

        zipDir.eraseFileOrDir()
    }

    @Throws(IOException::class)
    private fun initLogging() {
        val console = Database.log.console
        console.domains = LogDomain.ALL_DOMAINS
        console.level = LogLevel.DEBUG
    }
}