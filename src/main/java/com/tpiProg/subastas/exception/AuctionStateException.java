package com.tpiProg.subastas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AuctionStateException extends RuntimeException {

    public AuctionStateException(String message) {
        super(message);
    }
}