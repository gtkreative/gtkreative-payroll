al: Long)

private suspend fun calculatePayroll(employees: List<Employee>, dao: PayrollDao, type: String): List<PayrollRow> {
    val start = if (type == "MINGGUAN") weekStart() else monthStart(); val end = if (type == "MINGGUAN") weekEnd() else monthEnd(); val atts = dao.attendanceRange(fmt(start), fmt(end)); val byEmp = atts.groupBy { it.employeeId }
    return employees.map { e ->
        val a = byEmp[e.id].orEmpty(); val days = a.count { it.present }; val late = a.sumOf { it.lateMinutes }; val ot = a.sumOf { it.overtimeHours }; val otDays = a.count { it.overtimeHours > 0 }
        val base = if (type == "MINGGUAN") (e.baseSalary.toDouble() * days / e.standardWorkDays).roundToLong() else (e.baseSalary.toDouble() * days / e.standardWorkDays).roundToLong()
        val meal = e.mealDaily * days; val otp = (ot * e.overtimeRate).roundToLong(); val otm = e.mealOvertime * otDays; val loan = dao.loansRange(fmt(start), fmt(end)).filter { it.employeeId == e.id }.sumOf { it.amount }
        PayrollRow(e, days, late, ot, base, meal, otp, otm, loan, base + meal + otp + otm - loan)
    }
}

@Composable private fun PayrollCard(row: PayrollRow, share: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(row.employee.name, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(row.employee.position, color = Color.Gray, fontSize = 12.sp) }; Text(rupiah(row.total), fontWeight = FontWeight.Bold, color = Green) }
        Spacer(Modifier.height(10.dp)); Text("Hari ${row.days} • Lembur ${row.overtime} jam • Terlambat ${row.lateMinutes} menit", fontSize = 12.sp, color = Color.Gray)
        HorizontalDivider(Modifier.padding(vertical = 10.dp)); Text("Gaji pokok ${rupiah(row.base)}", fontSize = 13.sp); Text("Makan ${rupiah(row.meal)} + lembur ${rupiah(row.overtimePay)} + makan lembur ${rupiah(row.overtimeMeal)}", fontSize = 13.sp)
        Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = share) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Share Slip") }
    } }
}

@Composable private fun MoreScreen(company: CompanyProfile?, employees: List<Employee>, dao: PayrollDao, settings: () -> Unit, addLoan: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) { Text("Lainnya", fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)); Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text("Perusahaan", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(company?.name ?: "Belum diatur", color = Color.Gray); Spacer(Modifier.height(10.dp)); Button(onClick = settings, colors = ButtonDefaults.buttonColors(containerColor = Charcoal)) { Icon(Icons.Default.Business, null); Spacer(Modifier.width(8.dp)); Text("Pengaturan Perusahaan") }
    } }; Spacer(Modifier.height(14.dp)); Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Kasbon / Piutang", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Catat kasbon yang dapat menjadi potongan payroll.", color = Color.Gray, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); Button(onClick = addLoan, colors = ButtonDefaults.buttonColors(containerColor = Yellow), contentColor = Charcoal) { Icon(Icons.Default.CreditCard, null); Spacer(Modifier.width(8.dp)); Text("Tambah Kasbon") } } }; Spacer(Modifier.height(14.dp)); Text("Laporan gaji", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Gunakan menu Payroll → Laporan Gaji untuk rekap mingguan dan bulanan.", color = Color.Gray) }
}

@Composable private fun NumberField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier) { OutlinedTextField(value, onValue, label = { Text(label) }, modifier = modifier, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }

@Composable private fun AddEmployeeDialog(close: () -> Unit, save: (Employee) -> Unit) {
    var name by remember { mutableStateOf("") }; var position by remember { mutableStateOf("") }; var salary by remember { mutableStateOf("") }; var meal by remember { mutableStateOf("0") }; var otMeal by remember { mutableStateOf("0") }; var otRate by remember { mutableStateOf("0") }; var type by remember { mutableStateOf("WEEKLY") }
    AlertDialog(onDismissRequest = close, title = { Text("Tambah Karyawan") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Nama") }, singleLine = true); OutlinedTextField(position, { position = it }, label = { Text("Jabatan") }, singleLine = true); Row { FilterChip(type == "WEEKLY", { type = "WEEKLY" }, label = { Text("Mingguan") }); Spacer(Modifier.width(6.dp)); FilterChip(type == "MONTHLY", { type = "MON