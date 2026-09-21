package com.gtkreative.payroll.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Employee::class, Attendance::class, Loan::class, LoanSetting::class, CompanyProfile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PayrollDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS loan_settings (employeeId INTEGER NOT NULL, installment INTEGER NOT NULL, PRIMARY KEY(employeeId))") } }
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) { INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "gtkreative_payroll.db").addMigrations(MIGRATION_1_2).build().also { INSTANCE = it } }
    }
}
