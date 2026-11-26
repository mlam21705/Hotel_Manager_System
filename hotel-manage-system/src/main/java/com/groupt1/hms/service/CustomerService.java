package com.groupt1.hms.service;

import com.groupt1.hms.dto.CustomerBookingRequest;
import com.groupt1.hms.dto.CustomerProfileRequest;
import com.groupt1.hms.entity.Guest;
import com.groupt1.hms.entity.Reserve;
import com.groupt1.hms.entity.Room;
import com.groupt1.hms.entity.RoomType;

import java.time.LocalDate;
import java.util.List;

public interface CustomerService {
    Guest getGuestByUserId(Integer userId);
    List<Room> getAllRooms();
    List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut, Integer roomTypeId, Integer capacity, Integer branchId);
    List<RoomType> getRoomTypes();
    List<com.groupt1.hms.entity.Service> getServices();
    Room getRoom(Integer branchId, String roomNo);
    Reserve createReservation(Integer userId, CustomerBookingRequest request);
    List<Reserve> getReservations(Integer userId);
    Reserve getReservation(Integer userId, Integer reservationId);
    void cancelReservation(Integer userId, Integer reservationId);
    Guest updateProfile(Integer userId, CustomerProfileRequest request);
}
