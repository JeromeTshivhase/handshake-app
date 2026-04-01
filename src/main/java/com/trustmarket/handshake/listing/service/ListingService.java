package com.trustmarket.handshake.listing.service;

import com.trustmarket.handshake.listing.dto.*;
import com.trustmarket.handshake.listing.entity.*;
import com.trustmarket.handshake.listing.entity.Currency;
import com.trustmarket.handshake.listing.repository.*;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.shared.upload.FileStorageService;
import com.trustmarket.handshake.shared.upload.UploadedImage;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService;

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

        List<NormalizedImage> images = normalizeAndValidateImages(req.getImages(), req.getImageUrls());

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

        saveListingImages(listing, images);

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

        if (req.getImages() != null || req.getImageUrls() != null) {
            List<NormalizedImage> newImages = normalizeAndValidateImages(req.getImages(), req.getImageUrls());
            List<ListingImage> existingImages = listingImageRepository.findByListing_IdOrderByPositionAsc(listingId);
            deleteRemovedCloudinaryAssets(existingImages, newImages);
            listingImageRepository.deleteByListing_Id(listingId);
            saveListingImages(listing, newImages);
        }

        return toResponse(listing);
    }

    @Transactional
    public ListingResponse uploadImages(UUID sellerId, UUID listingId, MultipartFile[] files) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));
        assertOwnerAndEditable(sellerId, listing);

        List<ListingImage> existingImages = listingImageRepository.findByListing_IdOrderByPositionAsc(listingId);
        if (files == null || files.length == 0) {
            throw new BadRequestException("No files provided");
        }
        if (existingImages.size() + files.length > MAX_IMAGES) {
            throw new BadRequestException("No more than " + MAX_IMAGES + " images allowed");
        }

        List<UploadedImage> uploadedImages = fileStorageService.storeAllImages(files);
        try {
            int position = existingImages.size();
            for (UploadedImage uploadedImage : uploadedImages) {
                listingImageRepository.save(ListingImage.builder()
                        .listing(listing)
                        .url(uploadedImage.getUrl())
                        .publicId(uploadedImage.getPublicId())
                        .position(position++)
                        .build());
            }
        } catch (RuntimeException ex) {
            cleanupUploadedImages(uploadedImages);
            throw ex;
        }

        return toResponse(listing);
    }

    @Transactional
    public ListingResponse deleteImage(UUID sellerId, UUID listingId, UUID imageId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));
        assertOwnerAndEditable(sellerId, listing);

        ListingImage image = listingImageRepository.findByIdAndListing_Id(imageId, listingId)
                .orElseThrow(() -> new NotFoundException("Listing image not found: " + imageId));

        if (image.getPublicId() != null && !image.getPublicId().isBlank()) {
            fileStorageService.deleteByPublicId(image.getPublicId());
        }
        listingImageRepository.delete(image);

        List<ListingImage> remainingImages = listingImageRepository.findByListing_IdOrderByPositionAsc(listingId);
        int position = 0;
        for (ListingImage remainingImage : remainingImages) {
            remainingImage.setPosition(position++);
        }
        listingImageRepository.saveAll(remainingImages);

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

        List<ListingImage> listingImages = isActive
                ? listingImageRepository.findByListing_IdOrderByPositionAsc(listing.getId())
                : Collections.emptyList();

        List<String> images = listingImages.stream().map(ListingImage::getUrl).toList();
        List<ListingImageDto> imageDtos = listingImages.stream()
                .map(image -> ListingImageDto.builder()
                        .id(image.getId())
                        .url(image.getUrl())
                        .publicId(image.getPublicId())
                        .position(image.getPosition())
                        .build())
                .toList();

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
                .images(imageDtos)
                .imageUrls(images)
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }

    private List<NormalizedImage> normalizeAndValidateImages(List<ListingImageInput> images, List<String> legacyImageUrls) {
        boolean hasImageObjects = images != null && !images.isEmpty();
        boolean hasLegacyUrls = legacyImageUrls != null && !legacyImageUrls.isEmpty();

        if (hasImageObjects && hasLegacyUrls) {
            throw new BadRequestException("Provide either images or imageUrls, not both");
        }

        List<NormalizedImage> normalizedImages = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (hasImageObjects) {
            if (images.size() > MAX_IMAGES) throw new BadRequestException("No more than " + MAX_IMAGES + " images allowed");
            for (ListingImageInput image : images) {
                if (image == null) {
                    continue;
                }
                String normalizedUrl = normalizeAndValidateUrl(image.getUrl());
                if (normalizedUrl.isEmpty()) {
                    throw new BadRequestException("Each image must include a URL");
                }
                String publicId = image.getPublicId() == null ? "" : image.getPublicId().trim();
                if (publicId.isEmpty()) {
                    throw new BadRequestException("Each image must include a Cloudinary publicId");
                }
                String key = publicIdKey(publicId);
                if (seen.add(key)) {
                    normalizedImages.add(new NormalizedImage(normalizedUrl, publicId));
                }
            }
            return normalizedImages;
        }

        if (!hasLegacyUrls) {
            return normalizedImages;
        }

        if (legacyImageUrls.size() > MAX_IMAGES) throw new BadRequestException("No more than " + MAX_IMAGES + " images allowed");
        for (String url : legacyImageUrls) {
            String normalizedUrl = normalizeAndValidateUrl(url);
            if (!normalizedUrl.isEmpty()) {
                String key = urlKey(normalizedUrl);
                if (seen.add(key)) {
                    normalizedImages.add(new NormalizedImage(normalizedUrl, null));
                }
            }
        }
        return normalizedImages;
    }

    private void saveListingImages(Listing listing, List<NormalizedImage> images) {
        int pos = 0;
        for (NormalizedImage image : images) {
            listingImageRepository.save(ListingImage.builder()
                    .listing(listing)
                    .url(image.url())
                    .publicId(image.publicId())
                    .position(pos++)
                    .build());
        }
    }

    private void deleteRemovedCloudinaryAssets(List<ListingImage> existingImages, List<NormalizedImage> newImages) {
        Set<String> retainedKeys = new HashSet<>();
        for (NormalizedImage newImage : newImages) {
            retainedKeys.add(imageKey(newImage.url(), newImage.publicId()));
        }

        for (ListingImage existingImage : existingImages) {
            String existingKey = imageKey(existingImage.getUrl(), existingImage.getPublicId());
            if (!retainedKeys.contains(existingKey)
                    && existingImage.getPublicId() != null
                    && !existingImage.getPublicId().isBlank()) {
                fileStorageService.deleteByPublicId(existingImage.getPublicId());
            }
        }
    }

    private void cleanupUploadedImages(List<UploadedImage> uploadedImages) {
        for (UploadedImage uploadedImage : uploadedImages) {
            if (uploadedImage.getPublicId() != null && !uploadedImage.getPublicId().isBlank()) {
                try {
                    fileStorageService.deleteByPublicId(uploadedImage.getPublicId());
                } catch (RuntimeException ignored) {
                    // Best effort cleanup for partially-persisted uploads.
                }
            }
        }
    }

    private void assertOwnerAndEditable(UUID sellerId, Listing listing) {
        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("You can only edit your own listing");
        }
        if (listing.getStatus() == ListingStatus.SOLD) {
            throw new BadRequestException("Cannot edit a SOLD listing");
        }
    }

    private String normalizeAndValidateUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            throw new BadRequestException("Image URLs must start with http:// or https://");
        }
        try {
            new URL(trimmed);
        } catch (MalformedURLException e) {
            throw new BadRequestException("Invalid image URL: " + trimmed);
        }
        return trimmed;
    }

    private String imageKey(String url, String publicId) {
        return publicId != null && !publicId.isBlank() ? publicIdKey(publicId) : urlKey(url);
    }

    private String publicIdKey(String publicId) {
        return "publicId:" + publicId;
    }

    private String urlKey(String url) {
        return "url:" + url;
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private record NormalizedImage(String url, String publicId) {
    }
}