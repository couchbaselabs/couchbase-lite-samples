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

package com.couchbase.lite.color

import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.couchbase.lite.color.databinding.ActivityMainBinding
import com.couchbase.lite.color.model.MainViewModel
import com.couchbase.lite.color.model.SearchResult
import com.couchbase.lite.color.model.ServiceStatus
import com.couchbase.lite.color.service.NamedColor
import com.couchbase.lite.color.ui.ListViewAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private val model by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // List View:
        val listView = viewBinding.listView
        listView.adapter = ListViewAdapter()

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        listView.layoutManager = layoutManager

        val separator = DividerItemDecoration(listView.context, layoutManager.orientation)
        separator.setDrawable(ContextCompat.getDrawable(this, R.color.text)!!)
        listView.addItemDecoration(separator)

        val grey = ContextCompat.getColor(this, R.color.neutralGrey)

        val colorView = viewBinding.colorView
        val errorView = viewBinding.errorView
        val searchView = viewBinding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                colorView.setBackgroundColor(model.search(query)?.asInt() ?: grey)
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String) = false
        })
    }

    override fun onStart() {
        super.onStart()
        model.serviceStatus.observe(this, this::onServiceStatusChanged)
        model.searchResult.observe(this, this::onSearchResultChanged)
        model.start()
    }

    override fun onStop() {
        super.onStop()
        model.stop()
    }

    private fun onServiceStatusChanged(status: ServiceStatus) {
        viewBinding.statusView.text =
            when (status) {
                ServiceStatus.Stopped -> getResources().getString(R.string.stopped)
                ServiceStatus.Ready -> getResources().getString(R.string.ready)
                ServiceStatus.Failed -> getResources().getString(R.string.failed)
            }
    }

    private fun onSearchResultChanged(result: SearchResult) {
        displayResults(result.colors)
        viewBinding.errorView.text = result.error
    }

    private fun displayResults(colors: List<NamedColor>) =
        (viewBinding.listView.adapter as ListViewAdapter).updateItems(colors)
}