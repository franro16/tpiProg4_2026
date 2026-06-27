package com.tpiProg.subastas.service;

import com.tpiProg.subastas.dto.request.BidRequest;
import com.tpiProg.subastas.dto.response.BidResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface BidService {
    BidResponse placeBid(Long auctionId, BidRequest request, String username);
    List<BidResponse> getMyBids(Long auctionId, String username);
    List<BidResponse> getAuctionBids(Long auctionId, Authentication authentication);
}