package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerpilot.dto.ResumeExtractionResponse;
import com.careerpilot.exception.ResumeFileExtractionException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ResumeFileExtractionServiceTests {

    private final ResumeFileExtractionService service = new ResumeFileExtractionService();

    @Test
    void extractsTextAndSuggestedNameFromPdf() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Backend_Engineer_Resume.pdf",
                "application/pdf",
                pdfWithText("Jordan Lee - Java and Spring Engineer")
        );

        ResumeExtractionResponse response = service.extract(file);

        assertThat(response.suggestedName()).isEqualTo("Backend Engineer Resume");
        assertThat(response.content()).contains("Jordan Lee", "Java and Spring Engineer");
    }

    @Test
    void extractsTextAndSafeFilenameFromDocx() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "C:\\uploads\\Product_Resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxWithText("Product engineer with React experience")
        );

        ResumeExtractionResponse response = service.extract(file);

        assertThat(response.suggestedName()).isEqualTo("Product Resume");
        assertThat(response.content()).contains("Product engineer with React experience");
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> service.extract(file))
                .isInstanceOf(ResumeFileExtractionException.class)
                .hasMessage("Choose a PDF or DOCX resume to import.");
    }

    @Test
    void rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "Resume text".getBytes()
        );

        assertThatThrownBy(() -> service.extract(file))
                .isInstanceOf(ResumeFileExtractionException.class)
                .hasMessage("Only PDF and DOCX resume files are supported.");
    }

    @Test
    void rejectsFileLargerThanLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[(int) ResumeFileExtractionService.MAX_FILE_SIZE_BYTES + 1]
        );

        assertThatThrownBy(() -> service.extract(file))
                .isInstanceOf(ResumeFileExtractionException.class)
                .hasMessage("Resume files must be 5 MB or smaller.");
    }

    @Test
    void rejectsInvalidPdfSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "not a PDF".getBytes()
        );

        assertThatThrownBy(() -> service.extract(file))
                .isInstanceOf(ResumeFileExtractionException.class)
                .hasMessage("The selected file is not a valid PDF document.");
    }

    @Test
    void rejectsPdfWithoutReadableText() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scanned-resume.pdf",
                "application/pdf",
                emptyPdf()
        );

        assertThatThrownBy(() -> service.extract(file))
                .isInstanceOf(ResumeFileExtractionException.class)
                .hasMessageContaining("No readable text was found");
    }

    private static byte[] pdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static byte[] emptyPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private static byte[] docxWithText(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }
}
