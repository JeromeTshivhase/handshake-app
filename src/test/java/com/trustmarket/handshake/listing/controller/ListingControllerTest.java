package com.trustmarket.handshake.listing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.listing.dto.CreateListingRequest;
import com.trustmarket.handshake.listing.dto.ListingResponse;
import com.trustmarket.handshake.listing.dto.UpdateListingRequest;
import com.trustmarket.handshake.listing.entity.ListingCondition;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.service.ListingService;
import com.trustmarket.handshake.shared.exception.GlobalExceptionHandler;
import com.trustmarket.handshake.shared.security.OwnershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ListingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ListingService listingService;

    @Mock
    private OwnershipService ownershipService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ListingController(listingService, ownershipService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createAcceptsImageUrlsPayload() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(listingService.create(eq(sellerId), any(CreateListingRequest.class))).thenReturn(sampleResponse(listingId));

        CreateListingRequest request = CreateListingRequest.builder()
                .title("Vintage Tee")
                .description("Classic fit")
                .price(new BigDecimal("149.99"))
                .condition(ListingCondition.USED)
                .location("Johannesburg")
                .imageUrls(List.of("https://example.com/listings/one.jpg"))
                .build();

        mockMvc.perform(post("/api/listings")
                        .principal(authentication(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(listingId.toString()))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://example.com/listings/one.jpg"));

        verify(listingService).create(eq(sellerId), any(CreateListingRequest.class));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        UUID sellerId = UUID.randomUUID();

        CreateListingRequest request = CreateListingRequest.builder()
                .title(" ")
                .price(new BigDecimal("149.99"))
                .condition(ListingCondition.USED)
                .build();

        mockMvc.perform(post("/api/listings")
                        .principal(authentication(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.error").value("Validation Error"));
    }

    @Test
    void updateDelegatesToService() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        doNothing().when(ownershipService).assertListingOwner(listingId, sellerId);
        when(listingService.update(eq(sellerId), eq(listingId), any(UpdateListingRequest.class)))
                .thenReturn(sampleResponse(listingId));

        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Updated title")
                .imageUrls(List.of("https://example.com/listings/updated.jpg"))
                .build();

        mockMvc.perform(patch("/api/listings/{id}", listingId)
                        .principal(authentication(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(ownershipService).assertListingOwner(listingId, sellerId);
        verify(listingService).update(eq(sellerId), eq(listingId), any(UpdateListingRequest.class));
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        doNothing().when(ownershipService).assertListingOwner(listingId, sellerId);

        mockMvc.perform(delete("/api/listings/{id}", listingId)
                        .principal(authentication(sellerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(ownershipService).assertListingOwner(listingId, sellerId);
        verify(listingService).delete(sellerId, listingId);
    }

    private Authentication authentication(UUID userId) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null, "ROLE_USER");
        authentication.setAuthenticated(true);
        return authentication;
    }

    private ListingResponse sampleResponse(UUID listingId) {
        return ListingResponse.builder()
                .id(listingId)
                .title("Vintage Tee")
                .description("Classic fit")
                .price(new BigDecimal("149.99"))
                .currency("ZAR")
                .condition(ListingCondition.USED)
                .status(ListingStatus.ACTIVE)
                .location("Johannesburg")
                .sellerId(UUID.randomUUID())
                .sellerName("Test Seller")
                .sellerAvatar(null)
                .sellerVerified(false)
                .sellerTrustScore(0)
                .sellerRating(0.0)
                .sellerReviewCount(0)
                .imageUrls(List.of("https://example.com/listings/one.jpg"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

