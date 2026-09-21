package com.gtkreative.payroll.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: String,
    val salaryType: String = "WEEKLY",
    val baseSalary: Long,
    val mealDaily: Long = 0,
    val mealOvertime: Long = 0,
    val overtimeRate: Long = 0,
    val standardWorkDays: Int = 6,
    val active: Boolean = true
)

@Entity(tableName = "attendances", primaryKeys = ["employeeId", "date"])
data class Attendance(
    val employeeId: Long,
    val date: String,
    val present: Boolean = true,
    val lateMinutes: Int = 0,
    val overtimeHours: Double = 0.0
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val date: String,
    val amount: Long,
    val description: String = ""
)

@Entity(tableName = "company")
data class CompanyProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Nama Perusahaan",
    val address: String = "Alamat perusahaan",
    val contact: String = "",
    val logoUri: String? = null,
    val signatureUri: String? = null,
    val stampUri: String? = null
)


@Entity(tableName = "loan_settings")
data class LoanSetting(
    @PrimaryKey val employeeId: Long,
    val installment: Long = 0
)
