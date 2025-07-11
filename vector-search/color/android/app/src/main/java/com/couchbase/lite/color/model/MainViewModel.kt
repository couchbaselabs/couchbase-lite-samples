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

package com.couchbase.lite.color.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchbase.lite.color.service.AppService
import com.couchbase.lite.color.service.Color
import com.couchbase.lite.color.service.NamedColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ServiceStatus { Stopped, Ready, Failed }

sealed class SearchResult(val colors: List<NamedColor> = emptyList(), val error: String = "") {
    data object None : SearchResult()
    class Success(colors: List<NamedColor>) : SearchResult(colors = colors)
    class Failed(err: Exception) : SearchResult(error = err.message ?: "Unexpected error")
}


class MainViewModel(private val service: AppService) : ViewModel() {
    val serviceStatus = MutableLiveData(ServiceStatus.Stopped)
    val searchResult = MutableLiveData<SearchResult>(SearchResult.None)

    fun start() {
        if (serviceStatus.value == ServiceStatus.Ready) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                service.start()
                serviceStatus.postValue(ServiceStatus.Ready)
            } catch (e: Exception) {
                serviceStatus.postValue(ServiceStatus.Failed)
                searchResult.postValue(SearchResult.Failed(e))
            }
        }
    }

    fun search(query: String): Color? {
        val color: Color
        try {
            color = Color.parse(query)
        } catch (e: Exception) {
            searchResult.postValue(SearchResult.Failed(e))
            return null
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                searchResult.postValue(SearchResult.Success(service.search(color)))
            } catch (e: Exception) {
                searchResult.postValue(SearchResult.Failed(e))
            }
        }
        return color
    }

    fun stop() {
        viewModelScope.launch(Dispatchers.IO) { service.stop() }
        serviceStatus.postValue(ServiceStatus.Stopped)
    }
}
