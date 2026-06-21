package com.notifmanager

import android.app.Application
import com.notifmanager.data.AppDatabase
import com.notifmanager.data.AppSettings
import com.notifmanager.data.Repository

class NotifManagerApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val settings by lazy { AppSettings(this) }
    val repository by lazy { Repository(this, database.dao()) }
}
