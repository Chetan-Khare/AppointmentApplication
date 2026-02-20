package com.example.appointment.service;

import com.example.appointment.model.Appointment;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;


@Service
public class PdfService {
    public ByteArrayInputStream generateAdminReport(List<Appointment> appointments) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Report Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
            Paragraph title = new Paragraph("Clinic Appointment Summary Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Generated on: " + java.time.LocalDate.now()));
            document.add(new Paragraph("Total Records: " + appointments.size()));
            document.add(new Paragraph(" ")); // Spacer

            // 2. Table Construction (6 Columns)
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Column Widths
            float[] columnWidths = {1f, 2f, 2f, 1.5f, 1f, 1.5f};
            table.setWidths(columnWidths);

            // 3. Define Table Headers
            String[] headers = {"Token", "Patient", "Doctor", "Date", "Time", "Status"};
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.DARK_GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // 4. Fill Table Data
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (Appointment appt : appointments) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(appt.getTokenNumber()), dataFont)));
                table.addCell(new PdfPCell(new Phrase(appt.getPatientName(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(appt.getDoctorName(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(appt.getDate().toString(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(appt.getTime(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(appt.getStatus(), dataFont)));
            }

            document.add(table);
            document.close();

        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generatePrescriptionPdf(Appointment appointment) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Add Clinic Header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph header = new Paragraph("MEDICARE CLINIC", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
            Paragraph subHeader = new Paragraph("Official Medical Prescription", subHeaderFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            document.add(new Paragraph("\n")); // Spacer
            document.add(new Paragraph("---------------------------------------------------------------------------------------------------"));
            document.add(new Paragraph("\n"));

            // 2. Patient & Appointment Details
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("Token Number: #" + appointment.getTokenNumber(), labelFont));
            document.add(new Paragraph("Patient Name: " + appointment.getPatientName(), textFont));
            document.add(new Paragraph("Date: " + appointment.getDate(), textFont));

            // Handle Doctor Name (Use "Dr. Chetan" if null, or fetch from DB if available)
            String docName = (appointment.getDoctorName() != null) ? appointment.getDoctorName() : "Dr. Chetan";
            document.add(new Paragraph("Doctor: " + docName, textFont));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("---------------------------------------------------------------------------------------------------"));
            document.add(new Paragraph("\n"));

            // 3. The Prescription Body
            Paragraph rxHeader = new Paragraph("Rx / Diagnosis & Medicine:", labelFont);
            document.add(rxHeader);
            document.add(new Paragraph("\n"));

            String notes = (appointment.getPrescription() != null && !appointment.getPrescription().isEmpty())
                    ? appointment.getPrescription()
                    : "No specific notes provided.";

            Paragraph rxBody = new Paragraph(notes, textFont);
            document.add(rxBody);

            // 4. Footer
            document.add(new Paragraph("\n\n\n\n\n"));
            Paragraph footer = new Paragraph("Signed by: " + docName + "\n(Electronically Generated)", subHeaderFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}