from pathlib import Path
import re

p = Path("build-source/PayrollApp/app/src/main/java/com/gtkreative/payroll/MainActivity.kt")
s = p.read_text()

s = s.replace(
    "Surface(shape = RoundedCornerShape(24.dp), color = Charcoal, modifier = Modifier.fillMaxWidth()) { RoundedCornerShape(24.dp), color = Charcoal, modifier = Modifier.fillMaxWidth()) {",
    "Surface(shape = RoundedCornerShape(24.dp), color = Charcoal, modifier = Modifier.fillMaxWidth()) {"
)
s = s.replace(", )", ")")
s = s.replace('val base = if (type == "MINGGUAN") (e.baseSalary.toDouble() * days / e.standardWorkDays).roundToLong() else (e.baseSalary.toDouble() * days / e.standardWorkDays).roundToLong()', 'val base = (e.baseSalary.toDouble() * days / e.standardWorkDays.coerceAtLeast(1)).roundToLong()')

add = r'''@Composable private fun AddEmployeeDialog(close: () -> Unit, save: (Employee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf("0") }
    var otMeal by remember { mutableStateOf("0") }
    var otRate by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf("WEEKLY") }
    var showPreview by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("Tambah Karyawan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama") }, singleLine = true)
                OutlinedTextField(position, { position = it }, label = { Text("Jabatan") }, singleLine = true)
                Row {
                    FilterChip(type == "WEEKLY", { type = "WEEKLY" }, label = { Text("Mingguan") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(type == "MONTHLY", { type = "MONTHLY" }, label = { Text("Bulanan") })
                }
                NumberField(salary, { salary = it }, "Gaji pokok", Modifier.fillMaxWidth())
                NumberField(meal, { meal = it }, "Uang makan / hari", Modifier.fillMaxWidth())
                NumberField(otMeal, { otMeal = it }, "Makan lembur / hari", Modifier.fillMaxWidth())
                NumberField(otRate, { otRate = it }, "Tarif lembur / jam", Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = { showPreview = true },
                    enabled = (salary.toLongOrNull() ?: 0) > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Visibility, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Preview Slip Gaji")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        save(Employee(
                            name = name,
                            position = position,
                            salaryType = type,
                            baseSalary = salary.toLongOrNull() ?: 0,
                            mealDaily = meal.toLongOrNull() ?: 0,
                            mealOvertime = otMeal.toLongOrNull() ?: 0,
                            overtimeRate = otRate.toLongOrNull() ?: 0,
                            standardWorkDays = if (type == "MONTHLY") 26 else 6
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Batal") } }
    )

    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text("Preview Slip Gaji") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pe-Roll", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blue)
                    Text(if (type == "WEEKLY") "Periode: " + fmt(weekStart()) + " — " + fmt(weekEnd()) else "Periode: " + fmt(monthStart()) + " — " + fmt(monthEnd()), color = Color.Gray)
                    HorizontalDivider()
                    Text(if (name.isBlank()) "Nama Karyawan" else name, fontWeight = FontWeight.Bold)
                    Text(if (position.isBlank()) "Jabatan" else position, color = Color.Gray)
                    Text("Gaji pokok: " + rupiah(salary.toLongOrNull() ?: 0))
                    Text("Uang makan: " + rupiah(meal.toLongOrNull() ?: 0))
                    Text("Makan lembur: " + rupiah(otMeal.toLongOrNull() ?: 0))
                    Text("Tarif lembur/jam: " + rupiah(otRate.toLongOrNull() ?: 0))
                }
            },
            confirmButton = { Button(onClick = { showPreview = false }) { Text("Tutup") } }
        )
    }
}

'''
s = re.sub(r'@Composable private fun AddEmployeeDialog.*?@Composable private fun CompanyDialog', add + '@Composable private fun CompanyDialog', s, flags=re.S)

p.write_text(s)
print("final fixes applied")
