package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingRequestDTO;

import java.util.Set;

/** Approved per-passenger prices currently offered by the traveler booking form. */
final class TravelerExtrasPricing {
    private static final Set<String> BAGGAGE = Set.of("0", "2000", "3500", "5000");
    private static final Set<String> SEAT = Set.of("0", "2500", "5000", "7500");
    private static final Set<String> MEAL = Set.of("0", "1500", "2000", "3000", "4000");
    private static final Set<String> ASSISTANCE = Set.of("0", "2500", "3000", "4000", "5000");

    private TravelerExtrasPricing() {}

    static double calculate(BookingRequestDTO request) {
        int perPassenger = price(request.getBaggage(), BAGGAGE, "baggage")
                + price(request.getSeatPref(), SEAT, "seat preference")
                + price(request.getMeal(), MEAL, "meal")
                + price(request.getAssistance(), ASSISTANCE, "assistance");
        return (double) perPassenger * request.getPassengerCount();
    }

    private static int price(String selection, Set<String> approved, String name) {
        String value = selection == null ? "0" : selection;
        if (!approved.contains(value)) {
            throw new IllegalArgumentException("Invalid " + name + " selection");
        }
        return Integer.parseInt(value);
    }
}
