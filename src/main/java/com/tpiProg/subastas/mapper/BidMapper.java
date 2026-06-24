package com.tpiProg.subastas.mapper;

import com.tpiProg.subastas.domain.entity.Bid;
import com.tpiProg.subastas.dto.response.BidResponse;
import org.springframework.stereotype.Component;

@Component
public class BidMapper {

    public BidResponse toResponse(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuction().getId(),
                bid.getUser().getId(),
                bid.getUser().getUsername(),
                bid.getAmount(),
                bid.getBidDate()
        );
    }
}