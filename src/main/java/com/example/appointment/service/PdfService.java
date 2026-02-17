package com.example.appointment.service;

import com.example.appointment.model.Appointment;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

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