package com.trustmarket.handshake.listing.service;

import com.trustmarket.handshake.listing.dto.*;
import com.trustmarket.handshake.listing.entity.*;
import com.trustmarket.handshake.listing.entity.Currency;
import com.trustmarket.handshake.listing.repository.*;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private static final int MAX_IMAGES = 5;

    @Transactional
    public ListingResponse create(UUID sellerId, CreateListingRequest req) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller not found: " + sellerId));

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found: " + req.getCategoryId()));
        }

        List<String> imageUrls = normalizeAndValidateImages(req.getImageUrls());

        Listing listing = Listing.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .price(req.getPrice())
                .currency(Currency.ZAR)
                .condition(req.getCondition())
                .category(category)
                .seller(seller)
                .status(ListingStatus.ACTIVE)
                .location(req.getLocation())
                .build();

        listing = listingRepository.save(listing);

        int pos = 0;
        for (String url : imageUrls) {
            listingImageRepository.save(ListingImage.builder()
                    .listing(listing)
                    .url(url)
                    .position(pos++)
                    .build());
        }

        return toResponse(listing);
    }

    @Transactional(readOnly = true)
    public ListingResponse get(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));
        return toResponse(listing);
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> myListings(UUID sellerId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return listingRepository.findBySeller_Id(sellerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> browseActive(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return listingRepository.findByStatus(ListingStatus.ACTIVE, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> search(SearchListingsRequest req) {
        String keyword = blankToNull(req.getKeyword());
        if (keyword == null) keyword = "";
        String categoryName = blankToNull(req.getCategoryName());
        if (categoryName == null) categoryName = "";
        String location = blankToNull(req.getLocation());
        if (location == null) location = "";

        Sort sort = Sort.by("createdAt").descending();
        if ("price_asc".equalsIgnoreCase(req.getSortBy())) {
            sort = Sort.by("price").ascending();
        } else if ("price_desc".equalsIgnoreCase(req.getSortBy())) {
            sort = Sort.by("price").descending();
        }
        Pageable pageable = PageRequest.of(req.getPage(), req.getPageSize(), sort);

        Page<Listing> listings = listingRepository.searchActive(
                keyword,
                req.getCategoryId(),
                categoryName,
                req.getMinPrice(),
                req.getMaxPrice(),
                location,
                pageable
        );
        return listings.map(this::toResponse);
    }

    @Transactional
    public ListingResponse update(UUID sellerId, UUID listingId, UpdateListingRequest req) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("You can only edit your own listing");
        }
        if (listing.getStatus() == ListingStatus.SOLD) {
            throw new BadRequestException("Cannot edit a SOLD listing");
        }
        if (req.getTitle() != null) listing.setTitle(req.getTitle().trim());
        if (req.getDescription() != null) listing.setDescription(req.getDescription());
        if (req.getPrice() != null) listing.setPrice(req.getPrice());
        if (req.getCondition() != null) listing.setCondition(req.getCondition());
        if (req.getLocation() != null) listing.setLocation(req.getLocation().trim());
        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found: " + req.getCategoryId()));
            listing.setCategory(category);
        }
        if (req.getStatus() != null) {
            listing.setStatus(req.getStatus());
        }
        listing = listingRepository.save(listing);

        if (req.getImageUrls() != null) {
            List<String> newImages = normalizeAndValidateImages(req.getImageUrls());
            listingImageRepository.deleteByListing_Id(listingId);
            int pos = 0;
            for (String url : newImages) {
                listingImageRepository.save(ListingImage.builder()
                        .listing(listing)
                        .url(url)
                        .position(pos++)
                        .build());
            }
        }

        return toResponse(listing);
    }

    @Transactional
    public void delete(UUID sellerId, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));
        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("You can only delete your own listing");
        }
        // Soft-delete
        listing.setStatus(ListingStatus.INACTIVE);
        listingRepository.save(listing);
    }

    private ListingResponse toResponse(Listing listing) {
        Category cat = listing.getCategory();
        UUID catId = cat != null ? cat.getId() : null;
        String catName = cat != null ? cat.getName() : null;
        User seller = listing.getSeller();
        boolean isActive = listing.getStatus() == ListingStatus.ACTIVE;

        // Always load stored image URLs for a listing so the frontend can decide how to display
        // them (placeholders are a frontend concern). Previously images were hidden for
        // non-ACTIVE listings which caused images to be lost during handshake flows.
        List<String> images = listingImageRepository.findByListing_IdOrderByPositionAsc(listing.getId())
                .stream().map(ListingImage::getUrl).toList();

        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(isActive ? listing.getDescription() : "This listing is no longer public, but your deal is secure.")
                .price(listing.getPrice())
                .currency(listing.getCurrency().name())
                .condition(listing.getCondition())
                .categoryId(catId)
                .categoryName(catName)
                .sellerId(seller.getId())
                .sellerName(isActive ? seller.fullName() : null)
                .sellerAvatar(isActive ? seller.getProfilePictureUrl() : null)
                .sellerVerified(isActive && seller.isVerified())
                .sellerTrustScore(isActive ? seller.getTrustScore() : 0)
                .sellerRating(isActive ? seller.getRatingAverage() : 0.0)
                .sellerReviewCount(isActive ? seller.getRatingCount() : 0)
                .status(listing.getStatus())
                .location(isActive ? listing.getLocation() : null)
                .imageUrls(images)
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }

    private List<String> normalizeAndValidateImages(List<String> images) {
        if (images == null) return Collections.emptyList();
        if (images.size() > MAX_IMAGES) throw new BadRequestException("No more than " + MAX_IMAGES + " images allowed");
        Set<String> seen = new LinkedHashSet<>();
        for (String url : images) {
            String trimmed = url == null ? "" : url.trim();
            if (!trimmed.isEmpty()) {
                if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
                    throw new BadRequestException("Image URLs must start with http:// or https://");
                }
                try {
                    new URL(trimmed);
                } catch (MalformedURLException e) {
                    throw new BadRequestException("Invalid image URL: " + trimmed);
                }
                seen.add(trimmed);
            }
        }
        return new ArrayList<>(seen);
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}