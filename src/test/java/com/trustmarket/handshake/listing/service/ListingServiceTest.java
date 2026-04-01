package com.trustmarket.handshake.listing.service;

import com.trustmarket.handshake.listing.dto.CreateListingRequest;
import com.trustmarket.handshake.listing.dto.ListingResponse;
import com.trustmarket.handshake.listing.dto.UpdateListingRequest;
import com.trustmarket.handshake.listing.entity.Currency;
import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingCondition;
import com.trustmarket.handshake.listing.entity.ListingImage;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.CategoryRepository;
import com.trustmarket.handshake.listing.repository.ListingImageRepository;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private ListingImageRepository listingImageRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    private ListingService listingService;
    private User seller;
    private Listing listing;
    private List<ListingImage> storedImages;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(listingRepository, listingImageRepository, categoryRepository, userRepository);

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
            if (saved.getCurrency() == null) {
                saved.setCurrency(Currency.ZAR);
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
    void createPersistsImageUrlsAndReturnsThemInResponse() {
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));

        CreateListingRequest request = CreateListingRequest.builder()
                .title("Vintage Tee")
                .description("Classic fit")
                .price(new BigDecimal("149.99"))
                .condition(ListingCondition.USED)
                .location("Johannesburg")
                .imageUrls(List.of(
                        "https://cdn.example.com/listings/one.jpg",
                        "https://cdn.example.com/listings/two.jpg"
                ))
                .build();

        ListingResponse response = listingService.create(seller.getId(), request);

        assertEquals(2, storedImages.size());
        assertEquals(List.of("https://cdn.example.com/listings/one.jpg", "https://cdn.example.com/listings/two.jpg"),
                storedImages.stream().sorted(java.util.Comparator.comparing(ListingImage::getPosition)).map(ListingImage::getUrl).toList());
        assertEquals(List.of("https://cdn.example.com/listings/one.jpg", "https://cdn.example.com/listings/two.jpg"), response.getImageUrls());
    }

    @Test
    void createRejectsInvalidImageUrlScheme() {
        when(userRepository.findById(seller.getId())).thenReturn(Optional.of(seller));

        CreateListingRequest request = CreateListingRequest.builder()
                .title("Vintage Tee")
                .price(new BigDecimal("149.99"))
                .condition(ListingCondition.USED)
                .imageUrls(List.of("ftp://example.com/legacy.jpg"))
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> listingService.create(seller.getId(), request));

        assertEquals("Image URLs must start with http:// or https://", exception.getMessage());
    }

    @Test
    void updateReplacesStoredImagesWhenImageUrlsProvided() {
        storedImages.add(ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .url("https://cdn.example.com/listings/old.jpg")
                .position(0)
                .build());

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        UpdateListingRequest request = UpdateListingRequest.builder()
                .imageUrls(List.of(
                        "https://cdn.example.com/listings/new-1.jpg",
                        "https://cdn.example.com/listings/new-2.jpg"
                ))
                .build();

        ListingResponse response = listingService.update(seller.getId(), listing.getId(), request);

        verify(listingImageRepository).deleteByListing_Id(listing.getId());
        assertEquals(List.of("https://cdn.example.com/listings/new-1.jpg", "https://cdn.example.com/listings/new-2.jpg"),
                storedImages.stream().sorted(java.util.Comparator.comparing(ListingImage::getPosition)).map(ListingImage::getUrl).toList());
        assertEquals(2, response.getImageUrls().size());
    }
}

