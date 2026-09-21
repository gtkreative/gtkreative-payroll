 RoundedCornerShape(24.dp), color = Charcoal, modifier = Modifier.fillMaxWidth()) {
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
        Button(onClick = { selected?.let { scope.launch { dao.upsertAttendance(Attendance(it.id, fmt