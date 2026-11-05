package com.gabriele.iodice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Runs the whole application (controllers, validation, real converters, error handling)
/// with the sample messages of the assignment and with invalid ones.
@SpringBootTest
@AutoConfigureMockMvc
class FeedEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"msg_type":"odds_update","event_id":"ev123","values":{"1":2.0,"X":3.1,"2":3.8}}""",
            """
            {"msg_type":"settlement","event_id":"ev123","outcome":"1"}"""
    })
    void acceptsAlphaSampleMessages(String body) throws Exception {
        postJson("/provider-alpha/feed", body).andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"type":"ODDS","event_id":"ev456","odds":{"home":1.95,"draw":3.2,"away":4.0}}""",
            """
            {"type":"SETTLEMENT","event_id":"ev456","result":"away"}"""
    })
    void acceptsBetaSampleMessages(String body) throws Exception {
        postJson("/provider-beta/feed", body).andExpect(status().isNoContent());
    }

    @Test
    void rejectsAlphaOddsUpdateWithoutValues() throws Exception {
        postJson("/provider-alpha/feed", """
                {"msg_type":"odds_update","event_id":"ev1"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"values\" MUST be in the format")));
    }

    @Test
    void rejectsBetaOddsWithoutOdds() throws Exception {
        postJson("/provider-beta/feed", """
                {"type":"ODDS","event_id":"ev1"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"odds\" MUST be in the format")));
    }

    @Test
    void rejectsBetaSettlementWithoutResult() throws Exception {
        postJson("/provider-beta/feed", """
                {"type":"SETTLEMENT","event_id":"ev1"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("result is mandatory for settlement messages"));
    }

    @Test
    void rejectsUnknownBetaMessageType() throws Exception {
        postJson("/provider-beta/feed", """
                {"type":"FOO","event_id":"ev1"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Beta Feed message type not supported: FOO"));
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
