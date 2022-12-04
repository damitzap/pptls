from reportlab.pdfgen import canvas

report = canvas.Canvas("first_test.pdf")

report.drawString(50, 800, "**RESULTADOS DA PARTIDA**")
report.drawString()
report.save()