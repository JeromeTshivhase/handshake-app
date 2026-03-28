package com.trustmarket.handshake.favorite.service;

import com.trustmarket.handshake.favorite.entity.Favorite;
import com.trustmarket.handshake.favorite.repository.FavoriteRepository;
import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    @Transactional
    public void add(UUID userId, UUID listingId) {
        favoriteRepository.findByUser_IdAndListing_Id(userId, listingId).ifPresent(f -> { return; });

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Listing listing = listingRepository.findById(listingId).orElseThrow(() -> new NotFoundException("Listing not found"));

        favoriteRepository.save(Favorite.builder().user(user).listing(listing).build());
    }

    @Transactional
    public void remove(UUID userId, UUID listingId) {
        favoriteRepository.findByUser_IdAndListing_Id(userId, listingId)
                .ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public Page<Favorite> list(UUID userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return favoriteRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }
}