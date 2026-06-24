package com.tpiProg.subastas.mapper;

import com.tpiProg.subastas.domain.entity.Auction;
import com.tpiProg.subastas.dto.response.AuctionResponse;
import org.springframework.stereotype.Component;

@Component
public class AuctionMapper {

    public AuctionResponse toResponse(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getProduct().getId(),
                auction.getProduct().getName(),
                auction.getBasePrice(),
                auction.getCurrentPrice(),
                auction.getMinimumIncrement(),
                auction.getStartDate(),
                auction.getEndDate(),
                auction.getStatus().name(),
                auction.getDescription()
        );
    }
}