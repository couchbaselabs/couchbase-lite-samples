//
//  DBManager.h
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

#pragma once

#include "cbl/CouchbaseLite.h"

#include <exception>
#include <string>
#include <vector>

struct Color {
    std::string id;
    std::string name;
    double distance;
};

class DBManager {
public:
    struct Context {
        std::string databaseDir;
        std::string datasetDir;
        std::string extensionDir;
    };

    explicit DBManager(Context& context);

    ~DBManager() {
        CBLQuery_Release(_query);
        CBLCollection_Release(_collection);
        CBLDatabase_Release(_database);
    };

    std::vector<Color> search(std::vector<float> colorVector);
private:
    [[nodiscard]] CBLDatabase* loadDataset() const;

    Context _context;
    CBLDatabase *_database {};
    CBLCollection *_collection {};
    CBLQuery *_query {};
};

class CBLException : public std::exception {
public:
    explicit CBLException(const CBLError& error);

    [[nodiscard]] const char* what() const noexcept override { return _what.c_str(); }
private:
    std::string _what;
};