package com.gtkreative.payroll.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface PayrollDao {
    @Query("SELECT * FROM employees WHERE active = 1 ORDER BY name") fun employees(): Flow<List<Employee>>
    @Insert suspend fun insertEmployee(employee: Employee): Long
    @Update suspend fun updateEmployee(employee: Employee)
    @Delete suspend fun deleteEmployee(employee: Employee)

    @Query("SELECT * FROM attendances WHERE employeeId = :employeeId ORDER BY date DESC") fun attendance(employeeId: Long): Flow<List<Attendance>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAttendance(item: Attendance)
    @Query("SELECT * FROM attendances WHERE date BETWEEN :start AND :end") suspend fun attendanceRange(start: String, end: String): List<Attendance>
    @Query("DELETE FROM attendances WHERE date BETWEEN :start AND :end") suspend fun deleteAttendanceRange(start: String, end: String)

    @Query("SELECT * FROM loans WHERE employeeId = :employeeId ORDER BY date DESC") fun loans(employeeId: Long): Flow<List<Loan>>
    @Query("SELECT COALESCE(SUM(amount),0) FROM loans WHERE employeeId = :employeeId AND date <= :date") suspend fun loanBalance(employeeId: Long, date: String): Long
    @Query("SELECT COUNT(*) FROM loans WHERE employeeId = :employeeId AND date = :date AND description = :description") suspend fun loanTransactionExists(employeeId: Long, date: String, description: String): Int
    @Query("SELECT COALESCE(-SUM(amount),0) FROM loans WHERE employeeId = :employeeId AND date = :date AND description = :description") suspend fun loanPayment(employeeId: Long, date: String, description: String): Long
    @Query("SELECT * FROM loan_settings WHERE employeeId = :employeeId LIMIT 1") suspend fun loanSetting(employeeId: Long): LoanSetting?
    @Query("SELECT * FROM loan_settings WHERE employeeId = :employeeId LIMIT 1") fun loanSettingFlow(employeeId: Long): Flow<LoanSetting?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLoanSetting(setting: LoanSetting)
    @Insert suspend fun insertLoan(loan: Loan)
    @Query("SELECT COALESCE(SUM(amount),0) FROM loans WHERE employeeId = :employeeId") suspend fun loanTotal(employeeId: Long): Long
    @Query("SELECT * FROM loans WHERE date BETWEEN :start AND :end") suspend fun loansRange(start: String, end: String): List<Loan>

    @Query("SELECT * FROM company WHERE id = 1 LIMIT 1") fun company(): Flow<CompanyProfile?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCompany(company: CompanyProfile)
}
