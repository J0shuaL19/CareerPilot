package com.careerpilot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.careerpilot.config.OpenAiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiMatchAnalysisClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestsAndParsesStructuredAnalysis() throws JsonProcessingException {
        OpenAiProperties properties = properties("test-key");
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenAiMatchAnalysisClient client = new OpenAiMatchAnalysisClient(
                restClientBuilder.build(),
                objectMapper,
                properties
        );
        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().json("""
                        {
                          "model": "gpt-5.6",
                          "store": false,
                          "input": [
                            {"role": "system"},
                            {"role": "user"}
                          ],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "name": "career_match_analysis",
                              "strict": true,
                              "schema": {
                                "type": "object",
                                "additionalProperties": false
                              }
                            }
                          }
                        }
                        """))
                .andRespond(withSuccess(successfulResponse(), MediaType.APPLICATION_JSON));

        GeneratedMatchAnalysis result = client.analyze(input());

        assertThat(result.matchScore()).isEqualTo(87);
        assertThat(result.summary()).isEqualTo("Strong match.");
        assertThat(result.strengths()).isEqualTo("Java and PostgreSQL.");
        assertThat(result.gaps()).isEqualTo("Limited Kubernetes evidence.");
        assertThat(result.recommendations()).isEqualTo("Add deployment examples.");
        assertThat(result.modelName()).isEqualTo("gpt-5.6-2026-08-01");
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        OpenAiProperties properties = properties("");
        OpenAiMatchAnalysisClient client = new OpenAiMatchAnalysisClient(
                RestClient.create(properties.baseUrl().toString()),
                objectMapper,
                properties
        );

        assertThatThrownBy(() -> client.analyze(input()))
                .isInstanceOf(AiClientException.class)
                .hasMessage("OPENAI_API_KEY is not configured");
    }

    @Test
    void rejectsInvalidStructuredOutput() {
        OpenAiProperties properties = properties("test-key");
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.baseUrl().toString());
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenAiMatchAnalysisClient client = new OpenAiMatchAnalysisClient(
                restClientBuilder.build(),
                objectMapper,
                properties
        );
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess("""
                        {
                          "model": "gpt-5.6",
                          "output": [
                            {
                              "type": "message",
                              "content": [{"type": "output_text", "text": "not-json"}]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.analyze(input()))
                .isInstanceOf(AiClientException.class)
                .hasMessage("OpenAI returned invalid structured output");
        server.verify();
    }

    private static OpenAiProperties properties(String apiKey) {
        return new OpenAiProperties(
                apiKey,
                URI.create("https://api.openai.com/v1"),
                "gpt-5.6"
        );
    }

    private static MatchAnalysisInput input() {
        return new MatchAnalysisInput(
                "OpenAI",
                "Software Engineer",
                "Build reliable Java services.",
                "Backend Resume",
                "Experienced Java engineer."
        );
    }

    private String successfulResponse() throws JsonProcessingException {
        String structuredOutput = objectMapper.writeValueAsString(Map.of(
                "matchScore", 87,
                "summary", "Strong match.",
                "strengths", "Java and PostgreSQL.",
                "gaps", "Limited Kubernetes evidence.",
                "recommendations", "Add deployment examples."
        ));

        return objectMapper.writeValueAsString(Map.of(
                "model", "gpt-5.6-2026-08-01",
                "status", "completed",
                "output", List.of(Map.of(
                        "type", "message",
                        "content", List.of(Map.of(
                                "type", "output_text",
                                "text", structuredOutput
                        ))
                ))
        ));
    }
}
