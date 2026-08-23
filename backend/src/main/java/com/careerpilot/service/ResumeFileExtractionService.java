package com.careerpilot.service;

import com.careerpilot.dto.ResumeExtractionResponse;
import com.careerpilot.exception.ResumeFileExtractionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeFileExtractionService {

    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    static final int MAX_EXTRACTED_CHARACTERS = 100_000;

    public ResumeExtractionResponse extract(MultipartFile file) {
        validateFile(file);

        String filename = safeFilename(file.getOriginalFilename());
        String extension = extensionOf(filename);

        try {
            byte[] bytes = file.getBytes();
            String content = switch (extension) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                default -> throw unsupportedFile();
            };

            String normalizedContent = content.trim();
            if (normalizedContent.isBlank()) {
                throw new ResumeFileExtractionException(
                        "No readable text was found. Scanned PDFs without a text layer are not supported."
                );
            }
            if (normalizedContent.length() > MAX_EXTRACTED_CHARACTERS) {
                throw new ResumeFileExtractionException(
                        "The extracted resume is too long. Use a document with 100,000 characters or fewer."
                );
            }

            return new ResumeExtractionResponse(
                    suggestedName(filename, extension),
                    normalizedContent
            );
        } catch (ResumeFileExtractionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResumeFileExtractionException(
                    "We couldn't read this document. Make sure it is a valid PDF or DOCX file."
            );
        }
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeFileExtractionException("Choose a PDF or DOCX resume to import.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResumeFileExtractionException("Resume files must be 5 MB or smaller.");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String extension = extensionOf(filename);
        if (!extension.equals("pdf") && !extension.equals("docx")) {
            throw unsupportedFile();
        }
    }

    private static String extractPdf(byte[] bytes) throws IOException {
        if (!startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
            throw new ResumeFileExtractionException("The selected file is not a valid PDF document.");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String extractDocx(byte[] bytes) throws IOException {
        if (!isZipHeader(bytes)) {
            throw new ResumeFileExtractionException("The selected file is not a valid DOCX document.");
        }

        try (
                XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)
        ) {
            return extractor.getText();
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isZipHeader(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 'P'
                && bytes[1] == 'K'
                && (bytes[2] == 3 || bytes[2] == 5 || bytes[2] == 7)
                && (bytes[3] == 4 || bytes[3] == 6 || bytes[3] == 8);
    }

    private static String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "Imported Resume";
        }
        String normalized = originalFilename.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).trim();
    }

    private static String extensionOf(String filename) {
        int separator = filename.lastIndexOf('.');
        return separator < 0 ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private static String suggestedName(String filename, String extension) {
        int suffixLength = extension.isEmpty() ? 0 : extension.length() + 1;
        String baseName = filename.substring(0, filename.length() - suffixLength)
                .replace('_', ' ')
                .trim();
        if (baseName.isBlank()) {
            baseName = "Imported Resume";
        }
        return baseName.substring(0, Math.min(baseName.length(), 255));
    }

    private static ResumeFileExtractionException unsupportedFile() {
        return new ResumeFileExtractionException("Only PDF and DOCX resume files are supported.");
    }
}
