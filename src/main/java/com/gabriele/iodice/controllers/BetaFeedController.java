package com.gabriele.iodice.controllers;

import com.gabriele.iodice.dtos.BetaFeedDTO;
import com.gabriele.iodice.entities.StandardMessage;
import com.gabriele.iodice.services.ConverterService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class BetaFeedController {

    public ConverterService<BetaFeedDTO> converterService;

    public BetaFeedController(ConverterService<BetaFeedDTO> converterService) {
        this.converterService = converterService;
    }


    @PostMapping("/provider-beta/feed")
    public ResponseEntity<Void> betaService(@Valid @RequestBody BetaFeedDTO body){
        log.info("Provider Beta invoked {}", body);

        StandardMessage message = converterService.Convert(body);

        //logging the converted message as asked by the exercise
        log.info(message.toString());

        return ResponseEntity.noContent().build();
    }

}
