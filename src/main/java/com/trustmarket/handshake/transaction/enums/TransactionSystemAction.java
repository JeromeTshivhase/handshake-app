package com.trustmarket.handshake.transaction.enums;

public enum TransactionSystemAction {
    INITIATED,           // Transaction created
    PAYMENT_INITIATED,   // Payment gateway started
    PAYMENT_COMPLETED,   // Payment successful, escrow held
    MANUAL_COMPLETED,    // Buyer manually marked complete
    AUTO_RELEASED,       // System auto-released after hold period
    MANUALLY_DISPUTED,   // Buyer/seller initiated dispute
    ADMIN_REFUNDED       // Admin or system refund issued
}

