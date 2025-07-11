//
//  main.cpp
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
#include "RGB.h"

#include <iostream>
#include <sstream>
#include <string>

using namespace std;

int main() {
    DBManager::Context context{};
    context.databaseDir = "./";
    context.datasetDir = "./dataset";
    context.extensionDir = "./extension";

    auto db = DBManager(context);

    while (true) {
        string input;
        cout << "Enter color (ffffff or 255 255 255) > ";
        getline(cin, input);
        if (input == "quit") { break; }

        try {
            auto vector = RGB::colorVector(input);
            auto colors = db.search(vector);
            cout << endl;
            cout << "Found " << colors.size() << " matches:" << endl;
            for (const auto& color : colors) {
                cout << " * Color " << color.name << " (" << color.id << ") @ " << color.distance << endl;
            }
        } catch (const exception& e) {
            cout << endl;
            cout << "Error : " << e.what() << endl;
        }
        cout << endl;
    }

    return 0;
}
