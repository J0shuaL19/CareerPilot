package com.careerpilot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TimeConfigurationTests {

    @Test
    void createsClockInConfiguredBusinessTimeZone() {
        Clock clock = new TimeConfiguration().clock("America/Los_Angeles");

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
    }
}
