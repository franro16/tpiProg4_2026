package com.tpiProg.subastas.service;

import com.tpiProg.subastas.dto.request.AuctionRequest;
import com.tpiProg.subastas.dto.request.CancellationRequest;
import com.tpiProg.subastas.dto.response.AuctionDetailResponse;
import com.tpiProg.subastas.dto.response.AuctionResponse;
import com.tpiProg.subastas.dto.response.AuctionStateHistoryResponse;

import java.util.List;

public interface AuctionService {
    AuctionResponse create(AuctionRequest request, Long sellerId);
    AuctionResponse publish(Long auctionId, Long sellerId);
    AuctionDetailResponse getById(Long auctionId, Long requestingUserId);
    List<AuctionResponse> getAll();
    void cancel(Long auctionId, Long userId, boolean isAdmin, CancellationRequest request);
    List<AuctionStateHistoryResponse> getHistory(Long auctionId);
    void processScheduledTransitions();
}