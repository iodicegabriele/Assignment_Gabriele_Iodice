package com.gabriele.iodice.controllers;

import com.gabriele.iodice.dtos.AlphaFeedDTO;
import com.gabriele.iodice.entities.Outcome;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlphaFeedController.class)
class AlphaFeedControllerTest {

    private static final String URL = "/provider-alpha/feed";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConverterService<AlphaFeedDTO> converterService;

    @Test
    void acceptsOddsUpdateAndDelegatesToConverter() throws Exception {
        when(converterService.Convert(any()))
                .thenReturn(new OddsChangeMessage("ev123",
                        Map.of(Outcome.HOME, 2.0, Outcome.DRAW, 3.1, Outcome.AWAY, 3.8)));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"msg_type":"odds_update","event_id":"ev123","values":{"1":2.0,"X":3.1,"2":3.8}}
                        """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(converterService).Convert(new AlphaFeedDTO("odds_update", "ev123",
                Map.of("1", 2.0, "X", 3.1, "2", 3.8), null));
    }

    @Test
    void acceptsSettlementAndDelegatesToConverter() throws Exception {
        when(converterService.Convert(any())).thenReturn(new BetSettlementMessage("ev123", Outcome.HOME));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"msg_type":"settlement","event_id":"ev123","outcome":"1"}
                        """))
                .andExpect(status().isNoContent());

        verify(converterService).Convert(new AlphaFeedDTO("settlement", "ev123", null, "1"));
    }

    @Test
    void rejectsInvalidOutcomeWithBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"msg_type":"settlement","event_id":"ev123","outcome":"Y"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.outcome").value("Possible outcomes can only be 1|X|2"));

        verify(converterService, never()).Convert(any());
    }

    @Test
    void rejectsMissingMandatoryFieldsWithBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"values":{"1":2.0,"X":3.1,"2":3.8}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg_type").value("msg_type is mandatory"))
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
                .thenThrow(new IllegalArgumentException("Alpha Feed message type not supported"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"msg_type":"unknown","event_id":"ev123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("not supported")));
    }

    @Test
    void rejectsWrongHttpMethodWithMethodNotAllowed() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rejectsNonJsonContentWithUnsupportedMediaType() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("odds"))
                .andExpect(status().isUnsupportedMediaType());

        verify(converterService, never()).Convert(any());
    }

    @Test
    void mapsUnexpectedErrorsToInternalServerError() throws Exception {
        when(converterService.Convert(any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"msg_type":"settlement","event_id":"ev123","outcome":"1"}
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Generic Error please contact support."));
    }
}
