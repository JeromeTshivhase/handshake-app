package com.trustmarket.handshake.listing.service;

import com.trustmarket.handshake.listing.entity.Category;
import com.trustmarket.handshake.listing.repository.CategoryRepository;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    public Category get(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }
}