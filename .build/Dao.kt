package com.gtkreative.payroll.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface PayrollDao {
    @Query("SELECT * FROM employees WHERE active = 1 ORDER BY name") fun employees(): Flow<List<Employee>>
    @Insert suspend fun insertEmployee(employee: Employee)
    @Update suspend fun updateEmployee(employee: Employee)
    @Delete suspend fun deleteEmployee(employee: Employee)

    @Query("SELECT * FROM attendances WHERE employeeId = :employeeId ORDER BY date DESC") fun attendance(employeeId: Long): Flow<List<Attendance>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAttendance(item: Attendance)
    @Query("SELECT * FROM attendances WHERE date BETWEEN :start AND :end") suspend fun attendanceRange(start: String, end: String): List<Attendance>

    @Query("SELECT * FROM loans WHERE employeeId = :employeeId ORDER BY date DESC") fun loans(employeeId: Long): Flow<List<Loan>>
    @Insert suspend fun insertLoan(loan: Loan)
    @Query("SELECT COALESCE(SUM(amount),0) FROM loans WHERE employeeId = :employeeId") suspend fun loanTotal(employeeId: Long): Long
    @Query("SELECT * FROM loans WHERE date BETWEEN :start AND :end") suspend fun loansRange(start: String, end: String): List<Loan>

    @Query("SELECT * FROM company WHERE id = 1 LIMIT 1") fun company(): Flow<CompanyProfile?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCompany(company: CompanyProfile)
}
