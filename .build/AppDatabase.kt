package com.gtkreative.payroll.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Employee::class, Attendance::class, Loan::class, CompanyProfile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PayrollDao
    companion object { @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) { INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "gtkreative_payroll.db").build().also { INSTANCE = it } }
    }
}
