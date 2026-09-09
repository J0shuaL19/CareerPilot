package com.careerpilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerpilot.service.DataTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CareerPilotApplicationTests {

    @Autowired
    private DataTransferService dataTransferService;

    @Test
    void contextLoadsWithHostedImportDisabled() {
        assertThat(dataTransferService.capabilities().importEnabled()).isFalse();
    }
}
