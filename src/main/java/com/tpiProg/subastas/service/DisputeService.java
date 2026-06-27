package com.tpiProg.subastas.service;

import com.tpiProg.subastas.dto.request.DisputeRequest;
import com.tpiProg.subastas.dto.request.DisputeResolutionRequest;
import com.tpiProg.subastas.dto.response.DisputeResponse;

import java.util.List;

public interface DisputeService {
    DisputeResponse openDispute(DisputeRequest request, String username);
    DisputeResponse resolveDispute(Long disputeId, DisputeResolutionRequest request, String adminUsername);
    List<DisputeResponse> getAllDisputes();
    List<DisputeResponse> getMyDisputes(String username);
}