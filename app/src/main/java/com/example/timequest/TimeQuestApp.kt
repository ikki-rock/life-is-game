package com.example.timequest

import android.app.Application
import com.example.timequest.data.Store

class TimeQuestApp : Application() {
    val store: Store by lazy { Store(this) }
}
