package com.careerpilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:careerpilot-desktop-profile;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "logging.file.name=target/desktop-profile-test.log"
})
@ActiveProfiles("desktop")
class DesktopProfileTests {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    void startsWithDesktopProfileAndMigratedH2Database() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("desktop");

        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT COUNT(*) FROM flyway_schema_history WHERE version IS NOT NULL AND success = TRUE")) {
            assertThat(connection.getMetaData().getURL())
                    .startsWith("jdbc:h2:mem:careerpilot-desktop-profile");
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(10);
        }
    }
}
