package com.trustmarket.handshake.listing.controller;

import com.trustmarket.handshake.listing.dto.*;
import com.trustmarket.handshake.listing.service.ListingService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    /**
     * Create listing (seller = current user)
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create listing", operationId = "createListing")
    public ApiResponse<ListingResponse> create(Authentication auth, @Valid @RequestBody CreateListingRequest req) {
        UUID sellerId = AuthUtils.userId(auth);
        return ApiResponse.ok(listingService.create(sellerId, req));
    }

    /**
     * View listing (public)
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get listing by id", operationId = "getListingById")
    public ApiResponse<ListingResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(listingService.get(id));
    }

    /**
     * Browse active
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Browse active listings", operationId = "browseListings")
    public ApiResponse<Page<ListingResponse>> browse(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(listingService.browseActive(page, pageSize));
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Page<ListingResponse>> searchGet(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        SearchListingsRequest req = SearchListingsRequest.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .location(location)
                .sortBy(sortBy)
                .page(page)
                .pageSize(pageSize)
                .build();
        return ApiResponse.ok(listingService.search(req));
    }

    // POST for complex search
    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Page<ListingResponse>> searchPost(@RequestBody SearchListingsRequest req) {
        return ApiResponse.ok(listingService.search(req));
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get my listings", operationId = "getMyListings")
    public ApiResponse<Page<ListingResponse>> myListings(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        UUID sellerId = AuthUtils.userId(auth);
        return ApiResponse.ok(listingService.myListings(sellerId, page, pageSize));
    }

    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update listing", operationId = "updateListing")
    public ApiResponse<ListingResponse> update(Authentication auth, @PathVariable UUID id, @RequestBody UpdateListingRequest req) {
        UUID sellerId = AuthUtils.userId(auth);
        return ApiResponse.ok(listingService.update(sellerId, id, req));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete listing", operationId = "deleteListing")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable UUID id) {
        UUID sellerId = AuthUtils.userId(auth);
        listingService.delete(sellerId, id);
        return ApiResponse.ok(null);
    }
}