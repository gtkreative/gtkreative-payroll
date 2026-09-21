package com.gtkreative.payroll

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private val Blue = Color(0xFF1268E8)
private val Yellow = Color(0xFFFFC107)
private val Charcoal = Color(0xFF172B4D)
private val Soft = Color(0xFFF5F8FC)
private val Green = Color(0xFF168A5B)
private val Red = Color(0xFFC62828)

private fun rupiah(v: Long): String = "Rp" + String.format(Locale.US, "%,d", v).replace(',', '.')
private fun weekStart(): LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
private fun weekEnd(): LocalDate = weekStart().plusDays(6)
private fun monthStart(): LocalDate = LocalDate.now().withDayOfMonth(1)
private fun monthEnd(): LocalDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
private fun fmt(d: LocalDate) = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
private fun safeDate(value: String, fallback: LocalDate): LocalDate = runCatching { LocalDate.parse(value) }.getOrDefault(fallback)

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

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Yellow, background = Soft, surface = Color.White)) {
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

@Composable private fun AppHeader(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = Blue, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Badge, null, tint = Color.White) }
        }
        Spacer(Modifier.width(12.dp)); Column { Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Charcoal); Text(subtitle, color = Color.Gray) }
    }
}

@Composable private fun Dashboard(employees: List<Employee>) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        AppHeader("Pe-Roll", "Absensi Hari Ini, Gaji Nanti")
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = Charcoal, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text("PERIODE GAJI", color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${fmt(monthStart())} — ${fmt(monthEnd())}", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp)); Text("${employees.size} Karyawan Aktif", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Absensi dan payroll dalam satu tempat", color = Color.LightGray)
            }
        }
        Spacer(Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Karyawan", employees.size.toString(), Icons.Default.People, Modifier.weight(1f)); StatCard("Pe-Roll", "Absensi + Payroll", Icons.Default.Badge, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp)); Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) { Text("Workflow", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(10.dp));
                listOf("1. Input karyawan", "2. Masukkan total hari kerja per periode", "3. Catat keterlambatan, lembur & kasbon", "4. Generate payroll sesuai periode", "5. Preview → Share Slip Gaji").forEach { Text(it, Modifier.padding(vertical = 5.dp), color = Color.DarkGray) }
            }
        }
    }
}

@Composable private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = modifier) { Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = Blue); Spacer(Modifier.height(8.dp)); Text(value, fontWeight = FontWeight.Bold); Text(title, color = Color.Gray, fontSize = 12.sp) } }
}

@Composable private fun EmployeesScreen(employees: List<Employee>, add: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Karyawan", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Master data & preview slip", color = Color.Gray) }; FloatingActionButton(onClick = add, containerColor = Yellow, contentColor = Charcoal) { Icon(Icons.Default.Add, null) } }
        Spacer(Modifier.height(16.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(employees) { e -> EmployeeCard(e) } }
    }
}

@Composable private fun EmployeeCard(e: Employee) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(14.dp), color = Blue, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Text(e.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.White) } }
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(e.name, fontWeight = FontWeight.Bold); Text(e.position, color = Color.Gray, fontSize = 13.sp); Text("${e.salaryType} • ${rupiah(e.baseSalary)}", fontSize = 12.sp) }
        AssistChip(onClick = {}, label = { Text(if (e.active) "Aktif" else "Nonaktif") })
    } }
}

@Composable private fun AttendanceScreen(employees: List<Employee>, dao: PayrollDao) {
    var startText by remember { mutableStateOf(fmt(monthStart())) }
    var endText by remember { mutableStateOf(fmt(monthEnd())) }
    var days by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var late by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var overtime by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val start = safeDate(startText, monthStart()); val end = safeDate(endText, monthEnd())
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        AppHeader("Absensi", "Total hari kerja per periode gaji")
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(startText, { startText = it }, label = { Text("Mulai periode") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(endText, { endText = it }, label = { Text("Selesai periode") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Spacer(Modifier.height(10.dp)); Text("Masukkan total hari kerja tiap karyawan. Tidak perlu input satu per satu per tanggal.", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(employees) { e ->
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(e.name, fontWeight = FontWeight.Bold); Text(e.position, color = Color.Gray, fontSize = 12.sp) }; Text("${e.standardWorkDays} hari standar", color = Color.Gray, fontSize = 11.sp) }
                    Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumberField(days[e.id] ?: "", { days = days + (e.id to it) }, "Total hari kerja", Modifier.weight(1f), KeyboardType.Number)
                        NumberField(late[e.id] ?: "0", { late = late + (e.id to it) }, "Telat (menit)", Modifier.weight(1f), KeyboardType.Number)
                        NumberField(overtime[e.id] ?: "0", { overtime = overtime + (e.id to it) }, "Lembur (jam)", Modifier.weight(1f), KeyboardType.Decimal)
                    }
                } }
            }
        }
        Spacer(Modifier.height(10.dp)); Button(onClick = { scope.launch {
            employees.forEach { e -> val n = (days[e.id]?.toIntOrNull() ?: 0).coerceAtLeast(0); val l = late[e.id]?.toIntOrNull() ?: 0; val ot = overtime[e.id]?.toDoubleOrNull() ?: 0.0
                var d = start; var count = 0; while (!d.isAfter(end) && count < n) { dao.upsertAttendance(Attendance(e.id, fmt(d), true, if (count == 0) l else 0, if (count == 0) ot else 0.0)); d = d.plusDays(1); count++ }
            }
        } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue), enabled = employees.isNotEmpty()) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Simpan Absensi Periode") }
    }
}
