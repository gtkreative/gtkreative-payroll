package com.gtkreative.payroll

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.gtkreative.payroll.data.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private val Yellow = Color(0xFFFFC107)
private val Charcoal = Color(0xFF202124)
private val Soft = Color(0xFFF6F6F4)
private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)

private fun rupiah(v: Long): String = "Rp" + String.format(Locale.US, "%,d", v).replace(',', '.')
private fun weekStart(): LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
private fun weekEnd(): LocalDate = weekStart().plusDays(6)
private fun monthStart(): LocalDate = LocalDate.now().withDayOfMonth(1)
private fun monthEnd(): LocalDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
private fun fmt(d: LocalDate) = d.format(DateTimeFormatter.ISO_LOCAL_DATE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PayrollApp() } }
}

@Composable fun PayrollApp() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).dao() }
    val scope = rememberCoroutineScope()
    val employees by dao.employees().collectAsState(initial = emptyList())
    val company by dao.company().collectAsState(initial = null)
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showCompany by remember { mutableStateOf(false) }
    var showLoan by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Charcoal, secondary = Yellow, background = Soft, surface = Color.White)) {
        Scaffold(bottomBar = { BottomBar(tab) { tab = it } }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().background(Soft)) {
                when (tab) {
                    0 -> Dashboard(employees)
                    1 -> EmployeesScreen(employees, { showAdd = true })
                    2 -> AttendanceScreen(employees, dao)
                    3 -> PayrollScreen(employees, dao)
                    else -> MoreScreen(company, employees, dao, { showCompany = true }, { showLoan = true })
                }
                if (showAdd) AddEmployeeDialog({ showAdd = false }) { e -> scope.launch { dao.insertEmployee(e); showAdd = false } }
                if (showCompany) CompanyDialog(company ?: CompanyProfile(), { showCompany = false }) { c -> scope.launch { dao.saveCompany(c); showCompany = false } }
                if (showLoan) LoanDialog(employees, { showLoan = false }) { loan -> scope.launch { dao.insertLoan(loan); showLoan = false } }
            }
        }
    }
}

@Composable private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        listOf("Home" to Icons.Default.Home, "Karyawan" to Icons.Default.People, "Absensi" to Icons.Default.EventAvailable, "Payroll" to Icons.Default.Payments, "Lainnya" to Icons.Default.MoreHoriz).forEachIndexed { i, pair ->
            NavigationBarItem(selected = tab == i, onClick = { onTab(i) }, icon = { Icon(pair.second, null) }, label = { Text(pair.first, fontSize = 11.sp) })
        }
    }
}

@Composable private fun Dashboard(employees: List<Employee>) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Payroll", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Charcoal)
        Text("Ringkasan penggajian", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = Charcoal, modifier = Modifier.fillMaxWidth()) {