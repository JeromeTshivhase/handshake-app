package com.trustmarket.handshake.listing.entity;

public enum ListingStatus {
    ACTIVE,         // Shown/browsable, handshake(s) may be active
    PENDING_PAYMENT,// Under offer, awaiting payment
    SOLD,           // Marked as sold after completed transaction
    INACTIVE        // Hidden by owner or removed by admin
}