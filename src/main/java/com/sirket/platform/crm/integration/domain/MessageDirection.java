package com.sirket.platform.crm.integration.domain;

/**
 * Direction as seen from the company: a message received from the contact, or one sent to them.
 * It shows up in the timeline subject so the history reads correctly.
 */
public enum MessageDirection {
    INBOUND,
    OUTBOUND
}
