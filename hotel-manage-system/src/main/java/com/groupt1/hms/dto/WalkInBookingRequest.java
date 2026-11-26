package com.groupt1.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkInBookingRequest {
    private String guestName;
    private String cccd;
    private String phone;
    private String address;
    private Integer branchId;
    private String roomNo;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfGuest;
}
