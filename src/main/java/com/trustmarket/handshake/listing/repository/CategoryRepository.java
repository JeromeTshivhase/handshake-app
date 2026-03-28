package com.trustmarket.handshake.listing.repository;

import com.trustmarket.handshake.listing.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}