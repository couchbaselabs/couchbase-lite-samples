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

package com.couchbase.lite.color.app

import android.app.Application
import com.couchbase.lite.color.model.MainViewModel
import com.couchbase.lite.color.service.AppService
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

class ColorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable Koin dependency injection framework
        GlobalContext.startKoin {
            // inject Android context
            androidContext(this@ColorApp)

            // dependency register modules
            modules(
                module {
                    single { AppService(get()) }
                    viewModel { MainViewModel(get()) }
                })
        }
    }
}