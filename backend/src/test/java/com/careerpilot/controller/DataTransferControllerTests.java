package com.careerpilot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.DataTransferCapabilitiesResponse;
import com.careerpilot.dto.DataTransferCountsResponse;
import com.careerpilot.dto.DataTransferPreviewResponse;
import com.careerpilot.exception.DataTransferImportDisabledException;
import com.careerpilot.service.DataTransferFile;
import com.careerpilot.service.DataTransferService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DataTransferController.class)
class DataTransferControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataTransferService dataTransferService;

    @Test
    void returnsDesktopImportCapabilities() throws Exception {
        when(dataTransferService.capabilities())
                .thenReturn(new DataTransferCapabilitiesResponse(true, 1, 20L * 1024 * 1024));

        mockMvc.perform(get("/api/data-transfer/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importEnabled").value(true))
                .andExpect(jsonPath("$.formatVersion").value(1))
                .andExpect(jsonPath("$.maxImportFileSizeBytes").value(20L * 1024 * 1024));
    }

    @Test
    void downloadsFullDataExportAsJsonAttachment() throws Exception {
        byte[] content = "{\"format\":\"careerpilot-data\"}".getBytes();
        when(dataTransferService.exportData())
                .thenReturn(new DataTransferFile("careerpilot-data-20260908-200000.json", content));

        mockMvc.perform(get("/api/data-transfer/export"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(result -> assertThat(
                        result.getResponse().getHeader("Content-Disposition")
                ).isEqualTo(
                        "attachment; filename=\"careerpilot-data-20260908-200000.json\""
                ))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray())
                        .isEqualTo(content));
    }

    @Test
    void previewsUploadedDataExport() throws Exception {
        DataTransferCountsResponse incoming = new DataTransferCountsResponse(2, 1, 3, 1, 1, 2, 1);
        DataTransferCountsResponse existing = new DataTransferCountsResponse(0, 0, 0, 0, 1, 0, 0);
        when(dataTransferService.previewImport(any()))
                .thenReturn(new DataTransferPreviewResponse(
                        "careerpilot-data.json",
                        1,
                        Instant.parse("2026-09-08T20:00:00Z"),
                        incoming,
                        existing,
                        true
                ));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "careerpilot-data.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{}".getBytes()
        );

        mockMvc.perform(multipart("/api/data-transfer/import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("careerpilot-data.json"))
                .andExpect(jsonPath("$.incoming.jobs").value(2))
                .andExpect(jsonPath("$.incoming.jobActivities").value(3))
                .andExpect(jsonPath("$.willReplaceExistingData").value(true));
    }

    @Test
    void rejectsFullImportWhenNotRunningAsDesktopApp() throws Exception {
        when(dataTransferService.previewImport(any()))
                .thenThrow(new DataTransferImportDisabledException());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "careerpilot-data.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{}".getBytes()
        );

        mockMvc.perform(multipart("/api/data-transfer/import/preview").file(file))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Full data import is only available in the CareerPilot Windows app."
                ));
    }
}
