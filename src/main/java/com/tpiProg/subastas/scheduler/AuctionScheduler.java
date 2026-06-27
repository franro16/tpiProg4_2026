package com.tpiProg.subastas.scheduler;

import com.tpiProg.subastas.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionService auctionService;

    // Se ejecuta con un retraso fijo de 60 segundos (60000 ms) entre cada ejecución
    @Scheduled(fixedDelay = 60000)
    public void runAuctionTransitions() {
        log.debug("Iniciando tarea programada: Verificando transiciones de estado de subastas...");
        auctionService.processScheduledTransitions();
    }
}