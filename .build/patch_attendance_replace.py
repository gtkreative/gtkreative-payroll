from pathlib import Path
p=Path("build-source/PayrollApp/app/src/main/java/com/gtkreative/payroll/MainActivity.kt")
s=p.read_text()
s=s.replace('Column(Modifier.fillMaxSize().padding(20.dp)) {\n        Text("Absensi"', 'Column(Modifier.fillMaxSize().padding(20.dp)) {\n        Text("Absensi"')
s=s.replace('employees.forEach { e ->\n                val n=', 'dao.deleteAttendanceRange(fmt(start), fmt(end))\n            employees.forEach { e ->\n                val n=')
p.write_text(s)
