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
            Column(Modifier.padding(22.dp)) {
                Text("PERIODE MINGGU INI", color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${fmt(weekStart())} — ${fmt(weekEnd())}", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text("${employees.size} Karyawan Aktif", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Data payroll siap diproses", color = Color.LightGray)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Karyawan", employees.size.toString(), Icons.Default.People, Modifier.weight(1f))
            StatCard("Mode", "Mingguan / Bulanan", Icons.Default.CalendarMonth, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Workflow", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                listOf("1. Input karyawan", "2. Catat absensi, keterlambatan & lembur", "3. Catat kasbon/piutang", "4. Generate payroll", "5. Review → Final → Slip gaji").forEach { Text(it, Modifier.padding(vertical = 5.dp), color = Color.DarkGray) }
            }
        }
    }
}

@Composable private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = modifier) { Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = Charcoal); Spacer(Modifier.height(8.dp)); Text(value, fontWeight = FontWeight.Bold); Text(title, color = Color.Gray, fontSize = 12.sp) } }
}

@Composable private fun EmployeesScreen(employees: List<Employee>, add: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Karyawan", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Master data karyawan", color = Color.Gray) }; FloatingActionButton(onClick = add, containerColor = Yellow, contentColor = Charcoal) { Icon(Icons.Default.Add, null) } }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(employees) { e -> EmployeeCard(e) } }
    }
}

@Composable private fun EmployeeCard(e: Employee) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(14.dp), color = Yellow, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Text(e.name.take(1).uppercase(), fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(e.name, fontWeight = FontWeight.Bold); Text(e.position, color = Color.Gray, fontSize = 13.sp); Text("${e.salaryType} • ${rupiah(e.baseSalary)}", fontSize = 12.sp) }
        AssistChip(onClick = {}, label = { Text(if (e.active) "Aktif" else "Nonaktif") })
    } }
}

@Composable private fun AttendanceScreen(employees: List<Employee>, dao: PayrollDao) {
    var selected by remember { mutableStateOf<Employee?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var late by remember { mutableStateOf("0") }
    var overtime by remember { mutableStateOf("0") }
    var present by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Absensi", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Hari kerja, keterlambatan & lembur", color = Color.Gray); Spacer(Modifier.height(16.dp))
        EmployeeDropdown(employees, selected) { selected = it }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = fmt(date), onValueChange = {}, readOnly = true, label = { Text("Tanggal") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.CalendarToday, null) })
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(present, { present = it }); Text("Masuk kerja") }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NumberField(late, { late = it }, "Terlambat (menit)", Modifier.weight(1f)); NumberField(overtime, { overtime = it }, "Lembur (jam)", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { selected?.let { scope.launch { dao.upsertAttendance(Attendance(it.id, fmt(date), present, late.toIntOrNull() ?: 0, overtime.toDoubleOrNull() ?: 0.0)) } } }, enabled = selected != null, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Charcoal)) { Text("Simpan Absensi") }
        Spacer(Modifier.height(18.dp)); Text("Catatan: keterlambatan dicatat sebagai menit dan belum otomatis menjadi potongan sampai aturan potongan dikonfigurasi.", color = Color.Gray, fontSize = 12.sp)
    }
}

