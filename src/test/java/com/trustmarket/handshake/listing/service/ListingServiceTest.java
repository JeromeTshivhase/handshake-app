package com.trustmarket.handshake.listing.service;

import com.trustmarket.handshake.listing.dto.CreateListingRequest;
import com.trustmarket.handshake.listing.dto.ListingImageInput;
import com.trustmarket.handshake.listing.dto.ListingResponse;
import com.trustmarket.handshake.listing.dto.UpdateListingRequest;
import com.trustmarket.handshake.listing.entity.*;
import com.trustmarket.handshake.listing.repository.CategoryRepository;
import com.trustmarket.handshake.listing.repository.ListingImageRepository;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.upload.FileStorageService;
import com.trustmarket.handshake.shared.upload.UploadedImage;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private ListingImageRepository listingImageRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private MultipartFile multipartFile;

    private ListingService listingService;
    private User seller;
    private Listing listing;
    private List<ListingImage> storedImages;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(listingRepository, listingImageRepository, categoryRepository, userRepository, fileStorageService);
        seller = User.builder()
                .id(UUID.randomUUID())
                .email("seller@example.com")
                .passwordHash("secret")
                .firstName("Test")
                .lastName("Seller")
                .build();
        listing = Listing.builder()
                .id(UUID.randomUUID())
                .title("Vintage Tee")
                .description("Nice shirt")
                .price(new BigDecimal("199.99"))
                .currency(Currency.ZAR)
                .condition(ListingCondition.USED)
                .seller(seller)
                .status(ListingStatus.ACTIVE)
                .location("Cape Town")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        storedImages = new ArrayList<>();

        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            if (saved.getSeller() == null) {
                saved.setSeller(seller);
            }
            if (saved.getStatus() == null) {
                saved.setStatus(ListingStatus.ACTIVE);
            }
            return saved;
        });
        when(listingImageRepository.save(any(ListingImage.class))).thenAnswer(invocation -> {
            ListingImage image = invocation.getArgument(0);
            if (image.getId() == null) {
                image.setId(UUID.randomUUID());
            }
            storedImages.removeIf(existing -> existing.getId().equals(image.getId()));
            storedImages.add(image);
            return image;
        });
        when(listingImageRepository.findByListing_IdOrderByPositionAsc(any(UUID.class))).thenAnswer(invocation -> storedImages.stream()
                .sorted(java.util.Comparator.comparing(ListingImage::getPosition))
                .toList());
        doAnswer(invocation -> {
            storedImages.clear();
            return null;
        }).when(listingImageRepository).deleteByListing_Id(any(UUID.class));
        doAnswer(invocation -> {
            ListingImage image = invocation.getArgument(0);
            storedImages.removeIf(existing -> existing.getId().equals(image.getId()));
            return null;
        }).when(listingImageRepository).delete(any(ListingImage.class));
        when(listingImageRepository.saveAll(anyIterable())).thenAnswer(invocation -> {
            Iterable<ListingImage> iterable = invocation.getArgument(0);
            List<ListingImage> images = new ArrayList<>();
            storedImages.clear();
            for (ListingImage image : iterable) {
                images.add(image);
                storedImages.add(image);
            }
            return images;
        });
    }

    @Test
    void createPersistsCloudinaryMetadataAndReturnsImages() {
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));

        CreateListingRequest request = CreateListingRequest.builder()
                .title("Vintage Tee")
                .description("Classic fit")
                .price(new BigDecimal("149.99"))
                .condition(ListingCondition.USED)
                .location("Johannesburg")
                .images(List.of(
                        ListingImageInput.builder().url("https://res.cloudinary.com/demo/image/upload/v1/listings/one.jpg").publicId("listings/one").build(),
                        ListingImageInput.builder().url("https://res.cloudinary.com/demo/image/upload/v1/listings/two.jpg").publicId("listings/two").build()
                ))
                .build();

        ListingResponse response = listingService.create(seller.getId(), request);

        assertEquals(2, storedImages.size());
        assertEquals(List.of("listings/one", "listings/two"), storedImages.stream().map(ListingImage::getPublicId).toList());
        assertNotNull(response.getImages());
        assertEquals(2, response.getImages().size());
        assertEquals("listings/one", response.getImages().get(0).getPublicId());
        assertEquals(List.of(
                "https://res.cloudinary.com/demo/image/upload/v1/listings/one.jpg",
                "https://res.cloudinary.com/demo/image/upload/v1/listings/two.jpg"
        ), response.getImageUrls());
    }

    @Test
    void createRejectsMixedLegacyAndCloudinaryImagePayloads() {
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));

        CreateListingRequest request = CreateListingRequest.builder()
                .title("Vintage Tee")
                .price(new BigDecimal("149.99"))
                .condition(ListingCondition.USED)
                .images(List.of(ListingImageInput.builder()
                        .url("https://res.cloudinary.com/demo/image/upload/v1/listings/one.jpg")
                        .publicId("listings/one")
                        .build()))
                .imageUrls(List.of("https://example.com/legacy.jpg"))
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> listingService.create(seller.getId(), request));

        assertEquals("Provide either images or imageUrls, not both", exception.getMessage());
    }

    @Test
    void updateDeletesRemovedCloudinaryImagesAndReplacesStoredImages() {
        ListingImage removed = ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .url("https://res.cloudinary.com/demo/image/upload/v1/listings/old.jpg")
                .publicId("listings/old")
                .position(0)
                .build();
        ListingImage kept = ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .url("https://res.cloudinary.com/demo/image/upload/v1/listings/keep.jpg")
                .publicId("listings/keep")
                .position(1)
                .build();
        storedImages.add(removed);
        storedImages.add(kept);

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        UpdateListingRequest request = UpdateListingRequest.builder()
                .images(List.of(
                        ListingImageInput.builder().url("https://res.cloudinary.com/demo/image/upload/v1/listings/keep.jpg").publicId("listings/keep").build(),
                        ListingImageInput.builder().url("https://res.cloudinary.com/demo/image/upload/v1/listings/new.jpg").publicId("listings/new").build()
                ))
                .build();

        ListingResponse response = listingService.update(seller.getId(), listing.getId(), request);

        verify(fileStorageService).deleteByPublicId("listings/old");
        verify(fileStorageService, never()).deleteByPublicId("listings/keep");
        assertEquals(List.of("listings/keep", "listings/new"), storedImages.stream()
                .sorted(java.util.Comparator.comparing(ListingImage::getPosition))
                .map(ListingImage::getPublicId)
                .toList());
        assertEquals(2, response.getImages().size());
    }

    @Test
    void uploadImagesAppendsCloudinaryAssetsToExistingListingImages() {
        ListingImage existing = ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .url("https://res.cloudinary.com/demo/image/upload/v1/listings/existing.jpg")
                .publicId("listings/existing")
                .position(0)
                .build();
        storedImages.add(existing);

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(fileStorageService.storeAllImages(any(MultipartFile[].class))).thenReturn(List.of(
                UploadedImage.builder().url("https://res.cloudinary.com/demo/image/upload/v1/listings/two.jpg").publicId("listings/two").build(),
                UploadedImage.builder().url("https://res.cloudinary.com/demo/image/upload/v1/listings/three.jpg").publicId("listings/three").build()
        ));

        ListingResponse response = listingService.uploadImages(seller.getId(), listing.getId(), new MultipartFile[]{multipartFile, multipartFile});

        assertEquals(3, storedImages.size());
        assertEquals(List.of(0, 1, 2), storedImages.stream()
                .sorted(java.util.Comparator.comparing(ListingImage::getPosition))
                .map(ListingImage::getPosition)
                .toList());
        assertEquals(3, response.getImages().size());
        verify(fileStorageService, never()).deleteByPublicId("listings/two");
    }

    @Test
    void deleteImageRemovesCloudinaryAssetAndReordersRemainingImages() {
        ListingImage first = ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .url("https://res.cloudinary.com/demo/image/upload/v1/listings/first.jpg")
                .publicId("listings/first")
                .position(0)
                .build();
        ListingImage second = ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .url("https://res.cloudinary.com/demo/image/upload/v1/listings/second.jpg")
                .publicId("listings/second")
                .position(1)
                .build();
        storedImages.add(first);
        storedImages.add(second);

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingImageRepository.findByIdAndListing_Id(first.getId(), listing.getId())).thenReturn(Optional.of(first));

        ListingResponse response = listingService.deleteImage(seller.getId(), listing.getId(), first.getId());

        verify(fileStorageService).deleteByPublicId("listings/first");
        assertEquals(1, storedImages.size());
        assertEquals("listings/second", storedImages.get(0).getPublicId());
        assertEquals(0, storedImages.get(0).getPosition());
        assertEquals(1, response.getImages().size());
        assertEquals("listings/second", response.getImages().get(0).getPublicId());
    }
}

