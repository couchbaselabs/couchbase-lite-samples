//
//  AppService.swift
//  Word
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
import NaturalLanguage
import CouchbaseLiteSwift

/// Word data model
struct Word : Identifiable {
    var id: String
    var word: String
    var distance: Double
}

/// App Service for searching similar words.
actor AppService {
    let kDatabaseName = "db"
    let kCollectionName = "words"
    let kIndexName = "words_index"
    
    var db: Database!
    var collection: Collection!
    var query: Query!
    
    let model = NLEmbedding.wordEmbedding(for: .english)!
    
    var initialized = false
    
    init() { }
    
    /// Initialize the database by loading the dataset into the database and creating a vector index.
    func initialize() throws {
        if (initialized) { return }
        
        // Load dataset:
        db = try self.loadDataset()
        
        // Get the words collection:
        collection = try db.collection(name: kCollectionName)!
        
        // Create a vector index:
        var config = VectorIndexConfiguration(expression: "vector", dimensions: 300, centroids: 8)
        config.metric = .cosine
        try collection.createIndex(withName: kIndexName, config: config)
        
        initialized = true
    }
    
    /// Search similar words.
    func search(_ word: String) throws -> [Word] {
        if (!initialized) { throw "Service is not initialized" }
        
        // Use Apple's Word Embedding ML model to get a vector for the word:
        guard let wordVector = model.vector(for: word.lowercased()) else {
            throw "No vector for '\(word)'"
        }
        
        // Create a query object:
        if (query == nil) {
            let sql = "SELECT meta().id, word, VECTOR_DISTANCE(\(kIndexName)) " +
                      "FROM \(collection.fullName) " +
                      "WHERE VECTOR_MATCH(\(kIndexName), $vector, 10)"
            query = try db.createQuery(sql)
        }
        
        // Set $vector parameter on the query:
        let params = Parameters()
        params.setValue(wordVector, forName: "vector")
        query.parameters = params
        
        // Execute search:
        let results = try query.execute()
        
        // Return results:
        var words: [Word] = []
        for result in results {
            let id = result.string(at: 0)!
            let word = result.string(at: 1)!
            let distance = result.double(at: 2)
            words.append(Word(id: id, word: word, distance: distance))
        }
        return words
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
