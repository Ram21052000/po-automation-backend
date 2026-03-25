package com.poautomation;

import com.poautomation.entity.PurchaseOrder;
import com.poautomation.util.PdfParserUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfParseSmokeTest {

    @Test
    void parseAllSamplePdfs_hasLineItems() throws Exception {
        String[] paths = new String[]{
                "C:\\Users\\ASUS\\Desktop\\AMP0200A_PrettyLittleThing_Multi_0070034454_v1.pdf",
                "C:\\Users\\ASUS\\Desktop\\NAD0200A_coast_CST Petite_BCC13582_Petite Printed Floral Mesh Midaxi Dress_0003429730_v3.pdf",
                "C:\\Users\\ASUS\\Desktop\\NAM002_boohoo_Dresses_HZZ53685_Linen Look Puff Sleeve Shirred Maxi Dres_0003433306_v1 (1).pdf"
        };

        PdfParserUtil parser = new PdfParserUtil();
        for (String p : paths) {
            File f = new File(p);
            assertTrue(f.exists(), "Missing sample pdf: " + p);

            try (FileInputStream fis = new FileInputStream(f)) {
                MockMultipartFile mf = new MockMultipartFile(
                        "file",
                        f.getName(),
                        "application/pdf",
                        fis
                );

                PurchaseOrder po = parser.parse(mf);
                System.out.println("Parsed PO: " + po.getPoNumber() + ", lines=" + (po.getLines() == null ? 0 : po.getLines().size()));
                assertTrue(po.getLines() != null && po.getLines().size() > 0, "Expected line items parsed for " + f.getName());
            }
        }
    }
}

