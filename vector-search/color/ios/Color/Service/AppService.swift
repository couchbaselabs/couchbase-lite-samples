//
//  AppService.swift
//  Color
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

import Foundation
import CouchbaseLiteSwift

/// Color data model
struct ColorObject : Identifiable {
    var id: String
    var name: String
    var rgb: [Int]
    var distance: Double
}

/// App Service for searching similar colors.
actor AppService {
    let kDatabaseName = "db"
    let kCollectionName = "colors"
    let kIndexName = "colors_index"
    
    var db: Database!
    var collection: Collection!
    var query: Query!
    
    var initialized = false
    
    init() { }
    
    /// Initialize the database by loading the dataset into the database and creating a vector index.
    func initialize() throws {
        if (initialized) { return }
        
        try Extension.enableVectorSearch()
        
        // Load dataset:
        db = try self.loadDataset()
        
        // Get the colors collection:
        collection = try db.collection(name: kCollectionName)!
        
        // Create a vector index:
        var config = VectorIndexConfiguration(expression: "colorvect_l2", dimensions: 3, centroids: 2)
        config.encoding = VectorEncoding.none
        try collection.createIndex(withName: kIndexName, config: config)
        
        initialized = true
    }
    
    /// Search similar colors.
    func search(_ color: [Int]) throws -> [ColorObject] {
        if (!initialized) { throw "Service is not initialized" }
        
        // Create a query object:
        if (query == nil) {
            let sql = "SELECT id, color, colorvect_l2, APPROX_VECTOR_DISTANCE(colorvect_l2, $vector) " +
                      "FROM \(collection.fullName) " +
                      "LIMIT 8"
            query = try db.createQuery(sql)
        }
        
        // Set $vector parameter on the query:
        let params = Parameters()
        params.setValue(color, forName: "vector")
        query.parameters = params
        
        // Execute the query:
        let results = try query.execute()
        
        // Return results:
        var colors: [ColorObject] = []
        for result in results {
            let id = result.string(at: 0)!
            let name = result.string(at: 1)!
            let rgb = result.array(at: 2)!.toArray() as! [Int]
            let distance = result.double(at: 3)
            colors.append(ColorObject(id: id, name: name, rgb: rgb, distance: distance))
        }
        return colors
    }
    
    // MARK: Dataset
    
    /// Load dataset into the app's database. Perform only once when the app's database hasn't been created yet.
    private func loadDataset() throws -> Database {
        if !Database.exists(withName: kDatabaseName) {
            let path = datasetPath(name: kDatabaseName)!
            try Database.copy(fromPath: path, toDatabase: kDatabaseName, withConfig: nil)
        }
        return try Database(name: kDatabaseName)
    }
    
    /// Return the dataset path.
    private func datasetPath(name: String) -> String? {
        let res = ("Dataset" as NSString).appendingPathComponent(name)
        return Bundle(for: Swift.type(of:self)).path(forResource: res, ofType: "cblite2")
    }
}
