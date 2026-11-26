package com.groupt1.hms.service.impl;

import com.groupt1.hms.dto.CustomerBookingRequest;
import com.groupt1.hms.dto.CustomerProfileRequest;
import com.groupt1.hms.entity.*;
import com.groupt1.hms.repository.*;
import com.groupt1.hms.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {
    private final GuestRepository guestRepository;
    private final CustomerRoomRepository customerRoomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CustomerReserveRepository customerReserveRepository;
    private final ReserveDetailRepository reserveDetailRepository;
    private final ServiceRepository serviceRepository;
    private final UseServiceRepository useServiceRepository;

    public CustomerServiceImpl(GuestRepository guestRepository,
                               CustomerRoomRepository customerRoomRepository,
                               RoomTypeRepository roomTypeRepository,
                               CustomerReserveRepository customerReserveRepository,
                               ReserveDetailRepository reserveDetailRepository,
                               ServiceRepository serviceRepository,
                               UseServiceRepository useServiceRepository) {
        this.guestRepository = guestRepository;
        this.customerRoomRepository = customerRoomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.customerReserveRepository = customerReserveRepository;
        this.reserveDetailRepository = reserveDetailRepository;
        this.serviceRepository = serviceRepository;
        this.useServiceRepository = useServiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Guest getGuestByUserId(Integer userId) {
        return guestRepository.findByUserAccount_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ khách hàng."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return customerRoomRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut,
                                           Integer roomTypeId, Integer capacity, Integer branchId) {
        validateDates(checkIn, checkOut);
        return customerRoomRepository.searchAvailableRooms(checkIn, checkOut, roomTypeId, capacity, branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomType> getRoomTypes() {
        return roomTypeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.groupt1.hms.entity.Service> getServices() {
        return serviceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Room getRoom(Integer branchId, String roomNo) {
        return customerRoomRepository.findById(new RoomId(branchId, roomNo))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));
    }

    @Override
    @Transactional
    public Reserve createReservation(Integer userId, CustomerBookingRequest request) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        if (request.getBranchId() == null || request.getRoomNo() == null || request.getRoomNo().isBlank()) {
            throw new IllegalArgumentException("Thông tin phòng không hợp lệ.");
        }
        if (request.getNumberOfGuest() == null || request.getNumberOfGuest() < 1) {
            throw new IllegalArgumentException("Số khách phải lớn hơn hoặc bằng 1.");
        }

        Guest guest = getGuestByUserId(userId);
        Room room = getRoom(request.getBranchId(), request.getRoomNo());
        if (request.getNumberOfGuest() > room.getRoomType().getCapacity()) {
            throw new IllegalArgumentException("Số khách vượt quá sức chứa của phòng.");
        }
        if (!customerRoomRepository.isRoomAvailable(request.getBranchId(), request.getRoomNo(),
                request.getCheckInDate(), request.getCheckOutDate())) {
            throw new IllegalArgumentException("Phòng vừa được đặt hoặc không còn trống trong khoảng ngày đã chọn.");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal basePrice = room.getRoomType().getBasePrice();
        BigDecimal roomPrice = basePrice.multiply(BigDecimal.valueOf(nights));

        Reserve reserve = new Reserve();
        reserve.setGuest(guest);
        reserve.setReservationStatus("Pending");
        reserve.setTotalAmount(roomPrice);
        reserve = customerReserveRepository.save(reserve);

        ReserveDetail detail = new ReserveDetail();
        detail.setReserve(reserve);
        detail.setRoom(room);
        detail.setCheckInDate(request.getCheckInDate());
        detail.setCheckOutDate(request.getCheckOutDate());
        detail.setNumberOfGuest(request.getNumberOfGuest());
        detail.setPrice(basePrice);
        detail = reserveDetailRepository.save(detail);

        BigDecimal total = roomPrice;
        List<UseService> selectedServices = new ArrayList<>();
        List<Integer> serviceIds = request.getServiceIds() == null ? List.of() : request.getServiceIds();
        for (Integer serviceId : serviceIds) {
            com.groupt1.hms.entity.Service hotelService = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ đã chọn."));
            UseService useService = new UseService();
            useService.setReserveDetail(detail);
            useService.setService(hotelService);
            useService.setQuantity(1);
            useService.setPriceAtTime(hotelService.getServicePrice());
            selectedServices.add(useServiceRepository.save(useService));
            total = total.add(hotelService.getServicePrice());
        }

        detail.setUseServices(selectedServices);
        reserve.setReserveDetails(new ArrayList<>(List.of(detail)));
        reserve.setTotalAmount(total);
        return customerReserveRepository.save(reserve);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserve> getReservations(Integer userId) {
        Guest guest = getGuestByUserId(userId);
        return customerReserveRepository.findByGuestIdOrderByReserveDateDesc(guest.getGuestId());
    }

    @Override
    @Transactional(readOnly = true)
    public Reserve getReservation(Integer userId, Integer reservationId) {
        Guest guest = getGuestByUserId(userId);
        Reserve reserve = customerReserveRepository
                .findOwnedReservationWithDetails(reservationId, guest.getGuestId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng."));

        // Initialize the second collection separately to avoid Hibernate MultipleBagFetchException.
        for (ReserveDetail detail : reserve.getReserveDetails()) {
            detail.getUseServices().size();
            for (UseService useService : detail.getUseServices()) {
                useService.getService().getServiceName();
            }
        }
        return reserve;
    }

    @Override
    @Transactional
    public void cancelReservation(Integer userId, Integer reservationId) {
        Reserve reserve = getReservation(userId, reservationId);
        String status = reserve.getReservationStatus();

        if ("Confirmed".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Đơn đặt phòng đã được lễ tân xác nhận. Vui lòng liên hệ trực tiếp với quầy lễ tân để được hỗ trợ hủy phòng.");
        }

        if (!"Pending".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Chỉ có thể tự hủy đơn khi đơn ở trạng thái Chờ xác nhận (Pending).");
        }

        if (reserve.getReserveDetails() == null || reserve.getReserveDetails().isEmpty()) {
            throw new IllegalArgumentException("Đơn đặt phòng không có thông tin phòng.");
        }

        // Removed the time limit check so Pending reservations can always be cancelled by the customer.

        reserve.setReservationStatus("Cancelled");
        customerReserveRepository.save(reserve);
    }

    @Override
    @Transactional
    public Guest updateProfile(Integer userId, CustomerProfileRequest request) {
        Guest guest = getGuestByUserId(userId);
        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống.");
        }
        if (request.getCccd() == null || request.getCccd().isBlank()) {
            throw new IllegalArgumentException("CCCD không được để trống.");
        }

        String normalizedCccd = request.getCccd().trim();
        guestRepository.findByCccd(normalizedCccd).ifPresent(existing -> {
            if (!existing.getGuestId().equals(guest.getGuestId())) {
                throw new IllegalArgumentException("CCCD đã được sử dụng bởi tài khoản khác.");
            }
        });

        guest.setGuestName(request.getGuestName().trim());
        guest.setCccd(normalizedCccd);
        guest.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        guest.setAddress(trimToNull(request.getAddress()));
        return guestRepository.save(guest);
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày nhận và trả phòng.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày nhận phòng không được ở trong quá khứ.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng.");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
