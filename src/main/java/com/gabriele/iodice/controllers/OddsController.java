package com.gabriele.iodice.controllers;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class OddsController {

    @PostMapping("/provider-alpha/feed")
    public ResponseEntity<Void> alphaService(){
        log.info("Provider Alpha invoked");

        //TODO

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/provider-beta/feed")
    public ResponseEntity<Void> betaService(){
        log.info("Provider Beta invoked");

        //TODO

        return ResponseEntity.noContent().build();
    }

}
