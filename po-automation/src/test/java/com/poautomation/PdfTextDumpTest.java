package com.poautomation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;

public class PdfTextDumpTest {

    @Test
    void dumpOnePdfForParsing() throws Exception {
        String[] paths = new String[]{
                "C:\\Users\\ASUS\\Desktop\\AMP0200A_PrettyLittleThing_Multi_0070034454_v1.pdf",
                "C:\\Users\\ASUS\\Desktop\\NAD0200A_coast_CST Petite_BCC13582_Petite Printed Floral Mesh Midaxi Dress_0003429730_v3.pdf",
                "C:\\Users\\ASUS\\Desktop\\NAM002_boohoo_Dresses_HZZ53685_Linen Look Puff Sleeve Shirred Maxi Dres_0003433306_v1 (1).pdf"
        };

        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                throw new IllegalStateException("Missing sample PDF at: " + path);
            }

            try (PDDocument doc = PDDocument.load(file)) {
                String text = new PDFTextStripper().getText(doc);
                System.out.println("========== PDF=" + file.getName() + " ==========");
                System.out.println("PDF_TEXT_LENGTH=" + text.length());
                System.out.println("CONTAINS_DOLLAR=" + text.contains("$"));
                System.out.println("CONTAINS_POUND=" + text.contains("£"));
                System.out.println("CONTAINS_GBP=" + text.toUpperCase().contains("GBP"));
                System.out.println("CONTAINS_USD=" + text.toUpperCase().contains("USD"));

                System.out.println("---- PDF START (first 700 chars) ----");
                System.out.println(text.substring(0, Math.min(700, text.length())));

                int poIdx = text.indexOf("Purchase Order");
                if (poIdx >= 0) {
                    System.out.println("---- Purchase Order snippet ----");
                    System.out.println(text.substring(poIdx, Math.min(poIdx + 2000, text.length())));
                }

                int eachIdx = text.indexOf("Each");
                if (eachIdx >= 0) {
                    int start = Math.max(0, eachIdx - 500);
                    System.out.println("---- Around 'Each' (qty/unit/net) ----");
                    System.out.println(text.substring(start, Math.min(start + 1500, text.length())));
                }
            }
        }
    }
}

