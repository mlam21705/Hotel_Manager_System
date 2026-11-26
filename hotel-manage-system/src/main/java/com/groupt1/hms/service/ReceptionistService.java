package com.groupt1.hms.service;

import com.groupt1.hms.dto.WalkInBookingRequest;
import com.groupt1.hms.entity.*;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Receptionist front desk operations.
 */
public interface ReceptionistService {

    /**
     * Get a list of rooms for the room grid view.
     * Optionally filtered by branch and room status.
     *
     * @param branchId     the branch ID, or null for all branches
     * @param statusFilter the status filter ("Trống", "Đang dọn", "Đang sử dụng", "Đang bảo trì"), or null/empty/"ALL" for all
     * @return list of matching rooms
     */
    List<Room> getRoomGrid(Integer branchId, String statusFilter);

    /**
     * Search reservations by guest name, phone, or status.
     *
     * @param guestName guest name search keyword (LIKE matching, case-insensitive)
     * @param phone     phone search keyword (substring matching)
     * @param status    reservation status ("Pending", "Confirmed", "Checked-In", "Checked-Out", "Cancelled")
     * @return list of matching reservations
     */
    List<Reserve> searchReservations(String guestName, String phone, String status);

    /**
     * Retrieve a reservation by its ID.
     *
     * @param id the reservation ID
     * @return optional containing the reservation if found
     */
    Optional<Reserve> getReservationById(Integer id);

    /**
     * Retrieve a reservation detail by its ID.
     *
     * @param id the reservation detail ID
     * @return optional containing the reserve detail if found
     */
    Optional<ReserveDetail> getReserveDetailById(Integer id);

    /**
     * Confirm a pending reservation after verifying deposit.
     *
     * @param reservationId the reservation ID to confirm
     * @return the updated reservation
     */
    Reserve confirmReservation(Integer reservationId);

    /**
     * Perform check-in for a reserve detail (room) under a reservation.
     * Sets reservation status to Checked-In and RoomStatus to "Đang sử dụng".
     *
     * @param reservationId   the reservation ID
     * @param reserveDetailId the reserve detail ID
     * @return the updated reservation
     */
    Reserve checkIn(Integer reservationId, Integer reserveDetailId);

    /**
     * Perform check-out for a reservation's room.
     * Computes the total bill (room nights * snapshot price + service usages),
     * creates an Invoice marked as paid, sets reservation status to Checked-Out,
     * and sets room status to "Đang dọn" (to be cleaned).
     *
     * @param reserveDetailId the reserve detail ID triggering checkout
     * @return the generated invoice
     */
    Invoice checkOut(Integer reserveDetailId);

    /**
     * Create a new walk-in reservation immediately checked in.
     *
     * @param request walk-in reservation details DTO
     * @return the created reservation
     */
    Reserve createWalkInReservation(WalkInBookingRequest request);

    /**
     * Get all active services available to add to stays.
     *
     * @return list of services
     */
    List<Service> getAllServices();

    /**
     * Log a service usage for a guest during their stay.
     *
     * @param reserveDetailId the stay's detail ID
     * @param serviceId       the service ID used
     * @param quantity        quantity consumed
     * @return the created UseService record
     */
    UseService addServiceToStay(Integer reserveDetailId, Integer serviceId, Integer quantity);

    /**
     * Retrieve an invoice by its ID.
     *
     * @param id the invoice ID
     * @return optional containing the invoice if found
     */
    Optional<Invoice> getInvoiceById(Integer id);

    /**
     * Mark a room's cleaning task as completed and set its status back to "Trống".
     *
     * @param branchId the branch ID of the room
     * @param roomNo   the room number
     */
    void finishCleaning(Integer branchId, String roomNo);

    /**
     * Get the active checked-in reservation currently occupying a specific room.
     *
     * @param branchId the branch ID of the room
     * @param roomNo   the room number
     * @return optional containing the reservation if found
     */
    Optional<Reserve> getActiveReservationByRoom(Integer branchId, String roomNo);

    /**
     * Extend a checked-in reservation room stay to a new check-out date.
     *
     * @param reserveDetailId the reserve detail ID
     * @param newCheckOutDate the new check-out date
     * @return the updated Reserve
     */
    Reserve extendRoom(Integer reserveDetailId, java.time.LocalDate newCheckOutDate);
}
