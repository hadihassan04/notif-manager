package com.tide.app

import android.app.Application
import com.tide.app.data.AppDatabase
import com.tide.app.data.AppSettings
import com.tide.app.data.Repository

class TideApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val settings by lazy { AppSettings(this) }
    val repository by lazy { Repository(this, database.dao(), settings) }
}
