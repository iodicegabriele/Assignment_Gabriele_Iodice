package com.gabriele.iodice.controllers;

import com.gabriele.iodice.dtos.BetaFeedDTO;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import com.gabriele.iodice.services.ConverterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BetaFeedController.class)
class BetaFeedControllerTest {

    private static final String URL = "/provider-beta/feed";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConverterService<BetaFeedDTO> converterService;

    @Test
    void acceptsOddsAndDelegatesToConverter() throws Exception {
        when(converterService.Convert(any()))
                .thenReturn(new OddsChangeMessage("ev456", new Double[]{1.95, 3.2, 4.0}));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"ODDS","event_id":"ev456","odds":{"home":1.95,"draw":3.2,"away":4.0}}
                        """))
                .andExpect(status().isNoContent());

        verify(converterService).Convert(new BetaFeedDTO("ODDS", "ev456",
                Map.of("home", 1.95, "draw", 3.2, "away", 4.0), null));
    }

    @Test
    void acceptsSettlementAndDelegatesToConverter() throws Exception {
        when(converterService.Convert(any())).thenReturn(new BetSettlementMessage("ev456", 2));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"SETTLEMENT","event_id":"ev456","result":"away"}
                        """))
                .andExpect(status().isNoContent());

        verify(converterService).Convert(new BetaFeedDTO("SETTLEMENT", "ev456", null, "away"));
    }

    @Test
    void rejectsInvalidResultWithBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"SETTLEMENT","event_id":"ev456","result":"1"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("Possible outcomes can only be home|draw|away"));

        verify(converterService, never()).Convert(any());
    }

    @Test
    void rejectsMissingMandatoryFieldsWithBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"","odds":{"home":1.95,"draw":3.2,"away":4.0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("type is mandatory"))
                .andExpect(jsonPath("$.event_id").value("event_id is mandatory"));

        verify(converterService, never()).Convert(any());
    }

    @Test
    void rejectsMalformedJsonWithBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest());

        verify(converterService, never()).Convert(any());
    }

    @Test
    void mapsConverterIllegalArgumentToBadRequest() throws Exception {
        when(converterService.Convert(any()))
                .thenThrow(new IllegalArgumentException("message type not supported"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"UNKNOWN","event_id":"ev456"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("not supported")));
    }
}
