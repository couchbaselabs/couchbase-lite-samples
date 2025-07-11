//
//  DBManager.cpp
//  color
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

#include "DBManager.h"
#include "fleece/Fleece.h"

#include <filesystem>
#include <sstream>
#include <utility>

using namespace std;
using namespace std::filesystem;

#define kDatasetFileName "db.cblite2"
#define kDatabaseName "db"
#define kCollectionName "colors"
#define kIndexName "colors_index"

// FLString from std::string
#define FLS(str) FLStr((str).c_str())

// std::string from FLString
#define STR(str) std::string(static_cast<const char *>((str).buf), (str).size)

DBManager::DBManager(Context& context) : _context(std::move(context)) {
    // Enable Vector Search
    CBLError error {};
    if (!CBL_EnableVectorSearch(FLS(_context.extensionDir), &error)) {
        throw domain_error("Vector Search Extension cannot be enabled");
    }

    // Load dataset:
    _database = loadDataset();

    // Get the colors collection:
    _collection = CBLDatabase_Collection(_database, FLStr(kCollectionName), kCBLDefaultScopeName, &error);
    if (!_collection) {
        if (error.code == 0) {
            throw domain_error("'colors' collection is not found");
        }
        throw CBLException(error);
    }

    // Create a vector index config:
    CBLVectorIndexConfiguration config{};
    config.expressionLanguage = kCBLN1QLLanguage;
    config.expression = FLStr("colorvect_l2");
    config.dimensions = 3;
    config.centroids = 2;
    config.encoding = CBLVectorEncoding_CreateNone();

    // Create a vector index with the config:
    bool success = CBLCollection_CreateVectorIndex(_collection, FLStr(kIndexName), config, &error);
    CBLVectorEncoding_Free(config.encoding);
    if (!success) {
        throw CBLException(error);
    }
}

vector<Color> DBManager::search(vector<float> colorVector) {
    CBLError error{};

    // Create a vector search query:
    if (!_query) {
        stringstream sql;
        sql << "SELECT id, color, APPROX_VECTOR_DISTANCE(colorvect_l2, $vector)"
            << "FROM " << kCollectionName << " "
            << "LIMIT 8";

        _query = CBLDatabase_CreateQuery(_database,kCBLN1QLLanguage,FLS(sql.str()),nullptr, &error);
        if (!_query) {
            throw CBLException(error);
        }
    }

    // Set the input color vector to $vector parameter of the query:
    auto colorArray = FLMutableArray_New();
    for (auto val : colorVector) {
        FLMutableArray_AppendFloat(colorArray, val);
    }

    auto params = FLMutableDict_New();
    FLMutableDict_SetArray(params, FLSTR("vector"), colorArray);
    CBLQuery_SetParameters(_query, params);

    FLMutableArray_Release(colorArray);
    FLMutableDict_Release(params);

    // Execute the query:
    auto results = CBLQuery_Execute(_query, &error);
    if (!results) {
        throw CBLException(error);
    }

    // Return query result:
    vector<Color> colors {};
    while (CBLResultSet_Next(results)) {
        Color c{};
        c.id = STR(FLValue_AsString(CBLResultSet_ValueAtIndex(results, 0)));
        c.name = STR(FLValue_AsString(CBLResultSet_ValueAtIndex(results, 1)));
        c.distance = FLValue_AsDouble(CBLResultSet_ValueAtIndex(results, 2));
        colors.push_back(c);
    }
    return colors;
}

CBLDatabase* DBManager::loadDataset() const {
    CBLDatabaseConfiguration config {};
    config.directory = FLS(_context.databaseDir);

    CBLError error {};
    if (!CBL_DatabaseExists(FLStr(kDatabaseName), config.directory)) {
        auto datasetDBPath = path(_context.datasetDir).append(kDatasetFileName);
        if (!CBL_CopyDatabase(FLS(datasetDBPath), FLStr(kDatabaseName), &config, &error)) {
            throw CBLException(error);
        }
    }

    auto db = CBLDatabase_Open(FLStr(kDatabaseName), &config, &error);
    if (!db) {
        throw CBLException(error);
    }
    return db;
}

CBLException::CBLException(const CBLError &error) {
    FLSliceResult message = CBLError_Message(&error);
    _what = STR(message);
    FLSliceResult_Release(message);
}
