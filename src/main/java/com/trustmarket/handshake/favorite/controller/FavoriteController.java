package com.trustmarket.handshake.favorite.controller;

import com.trustmarket.handshake.favorite.service.FavoriteService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{listingId}")
    public ApiResponse<Void> add(Authentication auth, @PathVariable UUID listingId) {
        favoriteService.add(AuthUtils.userId(auth), listingId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{listingId}")
    public ApiResponse<Void> remove(Authentication auth, @PathVariable UUID listingId) {
        favoriteService.remove(AuthUtils.userId(auth), listingId);
        return ApiResponse.ok(null);
    }
}