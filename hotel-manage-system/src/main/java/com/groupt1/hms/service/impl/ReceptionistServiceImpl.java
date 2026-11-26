package com.groupt1.hms.service.impl;

import com.groupt1.hms.dto.WalkInBookingRequest;
import com.groupt1.hms.entity.*;
import com.groupt1.hms.repository.*;
import com.groupt1.hms.service.ReceptionistService;
import org.springframework.beans.factory.annotation.Autowired;
import com.groupt1.hms.entity.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ReceptionistServiceImpl implements ReceptionistService {

    @Autowired
    private ReserveRepository reserveRepository;

    @Autowired
    private ReserveDetailRepository reserveDetailRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UseServiceRepository useServiceRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private CustomerRoomRepository customerRoomRepository;

    // ──────────────── Room Grid ────────────────

    @Override
    public List<Room> getRoomGrid(Integer branchId, String statusFilter) {
        List<Room> rooms;
        if (branchId != null) {
            rooms = roomRepository.findById_BranchId(branchId);
        } else {
            rooms = roomRepository.findAll();
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            String finalStatus = statusFilter.trim();
            rooms = rooms.stream()
                    .filter(r -> finalStatus.equalsIgnoreCase(r.getRoomStatus()))
                    .toList();
        }
        return rooms;
    }

    // ──────────────── Reservations Search ────────────────

    @Override
    public List<Reserve> searchReservations(String guestName, String phone, String status) {
        String queryStatus = (status == null || "ALL".equalsIgnoreCase(status.trim())) ? "" : status.trim();
        return reserveRepository.searchReservations(guestName, phone, queryStatus);
    }

    @Override
    public Optional<Reserve> getReservationById(Integer id) {
        return reserveRepository.findById(id);
    }

    @Override
    public Optional<ReserveDetail> getReserveDetailById(Integer id) {
        return reserveDetailRepository.findById(id);
    }

    // ──────────────── Confirm Reservation ────────────────

    @Override
    @Transactional
    public Reserve confirmReservation(Integer reservationId) {
        Reserve reserve = reserveRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn đặt phòng không tồn tại."));

        if (!"Pending".equalsIgnoreCase(reserve.getReservationStatus())) {
            throw new IllegalStateException("Chỉ có thể xác nhận đơn đặt phòng đang ở trạng thái Pending.");
        }

        reserve.setReservationStatus("Confirmed");
        return reserveRepository.save(reserve);
    }

    // ──────────────── Check-In ────────────────

    @Override
    @Transactional
    public Reserve checkIn(Integer reservationId, Integer reserveDetailId) {
        Reserve reserve = reserveRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn đặt phòng không tồn tại."));

        if (!"Confirmed".equalsIgnoreCase(reserve.getReservationStatus())) {
            throw new IllegalStateException("Chỉ có thể nhận phòng đối với đơn đặt phòng ở trạng thái Confirmed.");
        }

        ReserveDetail detail = reserveDetailRepository.findById(reserveDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn đặt phòng không tồn tại."));

        // Validate check-in date
        LocalDate today = LocalDate.now();
        if (today.isBefore(detail.getCheckInDate().minusDays(1))) {
            throw new IllegalArgumentException("Không thể nhận phòng sớm hơn ngày đặt quá 1 ngày (Ngày đặt: " + detail.getCheckInDate() + ").");
        }

        Room room = detail.getRoom();
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin phòng được gán.");
        }

        if (!"Trống".equalsIgnoreCase(room.getRoomStatus())) {
            throw new IllegalStateException("Phòng " + room.getId().getRoomNo() + " hiện tại không trống (Trạng thái: " + room.getRoomStatus() + "). Không thể nhận phòng.");
        }

        // Update room status
        room.setRoomStatus("Đang sử dụng");
        roomRepository.save(room);

        // Update reservation status
        reserve.setReservationStatus("Checked-In");
        return reserveRepository.save(reserve);
    }

    // ──────────────── Check-Out ────────────────

    @Override
    @Transactional
    public Invoice checkOut(Integer reserveDetailId) {
        ReserveDetail triggerDetail = reserveDetailRepository.findById(reserveDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn đặt phòng không tồn tại."));

        Reserve reserve = triggerDetail.getReserve();
        if (reserve == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin đơn đặt phòng liên kết.");
        }

        if (!"Checked-In".equalsIgnoreCase(reserve.getReservationStatus())) {
            throw new IllegalStateException("Chỉ có thể trả phòng đối với đơn đặt phòng ở trạng thái Checked-In.");
        }

        // Prevent duplicate invoices
        Optional<Invoice> existingInvoice = invoiceRepository.findByReserve_ReservationId(reserve.getReservationId());
        if (existingInvoice.isPresent()) {
            throw new IllegalStateException("Đơn đặt phòng này đã được xuất hoá đơn.");
        }

        // Calculate checkout breakdown
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ReserveDetail> details = reserveDetailRepository.findByReserve_ReservationId(reserve.getReservationId());
        
        for (ReserveDetail d : details) {
            // Room nights (minimum 1)
            long nights = ChronoUnit.DAYS.between(d.getCheckInDate(), d.getCheckOutDate());
            if (nights <= 0) {
                nights = 1;
            }
            BigDecimal roomCost = d.getPrice().multiply(BigDecimal.valueOf(nights));
            totalAmount = totalAmount.add(roomCost);

            // Services cost
            List<UseService> servicesUsed = useServiceRepository.findByReserveDetail_ReserveDetailId(d.getReserveDetailId());
            for (UseService us : servicesUsed) {
                BigDecimal serviceCost = us.getPriceAtTime().multiply(BigDecimal.valueOf(us.getQuantity()));
                totalAmount = totalAmount.add(serviceCost);
            }
        }

        // Create Invoice
        Invoice invoice = new Invoice();
        invoice.setReserve(reserve);
        invoice.setTotalAmount(totalAmount);
        invoice.setInvoiceStatus("Đã thanh toán");
        invoice.setInvoiceDate(LocalDateTime.now());
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Update reservation status
        reserve.setReservationStatus("Checked-Out");
        reserve.setTotalAmount(totalAmount);
        reserveRepository.save(reserve);

        // Set all rooms in this reservation to "Đang dọn" status and create cleaning tasks
        UserAccount housekeeper = userAccountRepository.findByUsername("housek01").orElse(null);
        if (housekeeper == null) {
            List<UserAccount> allUsers = userAccountRepository.findAll();
            for (UserAccount u : allUsers) {
                if (u.getRole() != null && "Housekeeper".equalsIgnoreCase(u.getRole().getRoleName())) {
                    housekeeper = u;
                    break;
                }
            }
        }

        for (ReserveDetail d : details) {
            Room room = d.getRoom();
            if (room != null) {
                room.setRoomStatus("Đang dọn");
                roomRepository.save(room);

                // Create a CleaningTask if none exists for this room on today's date
                if (housekeeper != null) {
                    boolean taskExists = cleaningTaskRepository.findByAssignedTo_Username(housekeeper.getUsername())
                            .stream()
                            .anyMatch(t -> t.getRoom() != null 
                                    && t.getRoom().getId().equals(room.getId()) 
                                    && LocalDate.now().equals(t.getTaskDate())
                                    && !"Completed".equalsIgnoreCase(t.getStatus()));
                    if (!taskExists) {
                        CleaningTask task = new CleaningTask();
                        task.setRoom(room);
                        task.setAssignedTo(housekeeper);
                        task.setTaskDate(LocalDate.now());
                        task.setStatus("Pending");
                        task.setNotes("Room checked out, needs cleaning.");
                        cleaningTaskRepository.save(task);
                    }
                }
            }
        }

        return savedInvoice;
    }

    // ──────────────── Walk-In Booking ────────────────

    @Override
    @Transactional
    public Reserve createWalkInReservation(WalkInBookingRequest request) {
        // Validate request
        if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên khách hàng không được để trống.");
        }
        if (request.getCccd() == null || request.getCccd().trim().isEmpty()) {
            throw new IllegalArgumentException("Số CCCD không được để trống.");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (request.getBranchId() == null || request.getRoomNo() == null || request.getRoomNo().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phòng để đặt.");
        }
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày nhận/trả phòng.");
        }
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng.");
        }
        if (request.getNumberOfGuest() == null || request.getNumberOfGuest() <= 0) {
            throw new IllegalArgumentException("Số lượng khách phải lớn hơn 0.");
        }

        // Get/Create Guest
        String cccd = request.getCccd().trim();
        Guest guest = guestRepository.findByCccd(cccd)
                .orElse(null);

        if (guest == null) {
            guest = new Guest();
            guest.setCccd(cccd);
            guest.setGuestName(request.getGuestName().trim());
            guest.setPhoneNumber(request.getPhone().trim());
            guest.setAddress(request.getAddress() != null ? request.getAddress().trim() : "");
            guest = guestRepository.save(guest);
        } else {
            // Update guest details if changed
            guest.setGuestName(request.getGuestName().trim());
            guest.setPhoneNumber(request.getPhone().trim());
            if (request.getAddress() != null) {
                guest.setAddress(request.getAddress().trim());
            }
            guest = guestRepository.save(guest);
        }

        // Load Room
        RoomId roomId = new RoomId(request.getBranchId(), request.getRoomNo().trim());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng đã chọn không tồn tại."));

        if (!"Trống".equalsIgnoreCase(room.getRoomStatus())) {
            throw new IllegalStateException("Phòng " + room.getId().getRoomNo() + " hiện không trống (Trạng thái: " + room.getRoomStatus() + ").");
        }

        // Check room capacity
        RoomType roomType = room.getRoomType();
        if (roomType == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin loại phòng.");
        }
        if (request.getNumberOfGuest() > roomType.getCapacity()) {
            throw new IllegalArgumentException("Số lượng khách vượt quá sức chứa tối đa của phòng (" + roomType.getCapacity() + " người).");
        }

        // Compute room price
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            nights = 1;
        }
        BigDecimal basePrice = roomType.getBasePrice();
        BigDecimal roomCharge = basePrice.multiply(BigDecimal.valueOf(nights));

        // Create Reserve
        Reserve reserve = new Reserve();
        reserve.setGuest(guest);
        reserve.setReservationStatus("Checked-In"); // Walk-in is checked in immediately
        reserve.setTotalAmount(roomCharge);
        reserve.setReserveDate(LocalDateTime.now());
        reserve = reserveRepository.save(reserve);

        // Create ReserveDetail
        ReserveDetail detail = new ReserveDetail();
        detail.setReserve(reserve);
        detail.setRoom(room);
        detail.setCheckInDate(request.getCheckInDate());
        detail.setCheckOutDate(request.getCheckOutDate());
        detail.setNumberOfGuest(request.getNumberOfGuest());
        detail.setPrice(basePrice);
        reserveDetailRepository.save(detail);

        // Update room status
        room.setRoomStatus("Đang sử dụng");
        roomRepository.save(room);

        return reserve;
    }

    // ──────────────── Services ────────────────

    @Override
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    @Transactional
    public UseService addServiceToStay(Integer reserveDetailId, Integer serviceId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng dịch vụ phải lớn hơn 0.");
        }

        ReserveDetail detail = reserveDetailRepository.findById(reserveDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn đặt phòng không tồn tại."));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại."));

        UseService useService = new UseService();
        useService.setReserveDetail(detail);
        useService.setService(service);
        useService.setQuantity(quantity);
        useService.setPriceAtTime(service.getServicePrice());
        useService.setUseDate(LocalDateTime.now());

        return useServiceRepository.save(useService);
    }

    @Override
    public Optional<Invoice> getInvoiceById(Integer id) {
        return invoiceRepository.findById(id);
    }

    @Override
    @Transactional
    public void finishCleaning(Integer branchId, String roomNo) {
        RoomId roomId = new RoomId(branchId, roomNo);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại."));
        if ("Đang dọn".equalsIgnoreCase(room.getRoomStatus())) {
            room.setRoomStatus("Trống");
            roomRepository.save(room);
        }
    }

    @Override
    public Optional<Reserve> getActiveReservationByRoom(Integer branchId, String roomNo) {
        List<Reserve> list = reserveRepository.findCheckedInReservationsByRoom(branchId, roomNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    @Transactional
    public Reserve extendRoom(Integer reserveDetailId, LocalDate newCheckOutDate) {
        ReserveDetail detail = reserveDetailRepository.findById(reserveDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn đặt phòng không tồn tại."));

        Reserve reserve = detail.getReserve();
        if (reserve == null || !"Checked-In".equalsIgnoreCase(reserve.getReservationStatus())) {
            throw new IllegalStateException("Chỉ có thể gia hạn cho phòng đang lưu trú (Checked-In).");
        }

        if (newCheckOutDate.isBefore(detail.getCheckOutDate()) || newCheckOutDate.isEqual(detail.getCheckOutDate())) {
            throw new IllegalArgumentException("Ngày trả phòng mới phải sau ngày trả phòng hiện tại (" + detail.getCheckOutDate() + ").");
        }

        // Check if the room is available for the extension period
        // The checkIn date for the availability query is the current checkOutDate
        boolean available = customerRoomRepository.isRoomAvailable(
                detail.getRoom().getId().getBranchId(),
                detail.getRoom().getId().getRoomNo(),
                detail.getCheckOutDate(),
                newCheckOutDate
        );

        if (!available) {
            throw new IllegalStateException("Phòng đã được đặt trong khoảng thời gian gia hạn. Vui lòng chọn phòng khác cho khách hoặc thay đổi ngày.");
        }

        // Update the checkout date
        detail.setCheckOutDate(newCheckOutDate);
        reserveDetailRepository.save(detail);

        // Recalculate totalAmount for the Reserve
        BigDecimal newTotalAmount = BigDecimal.ZERO;
        List<ReserveDetail> allDetails = reserveDetailRepository.findByReserve_ReservationId(reserve.getReservationId());
        for (ReserveDetail d : allDetails) {
            // Room cost
            long nights = ChronoUnit.DAYS.between(d.getCheckInDate(), d.getCheckOutDate());
            if (nights <= 0) nights = 1;
            BigDecimal roomCost = d.getPrice().multiply(BigDecimal.valueOf(nights));
            newTotalAmount = newTotalAmount.add(roomCost);

            // Services cost
            List<UseService> servicesUsed = useServiceRepository.findByReserveDetail_ReserveDetailId(d.getReserveDetailId());
            for (UseService us : servicesUsed) {
                BigDecimal serviceCost = us.getPriceAtTime().multiply(BigDecimal.valueOf(us.getQuantity()));
                newTotalAmount = newTotalAmount.add(serviceCost);
            }
        }

        reserve.setTotalAmount(newTotalAmount);
        return reserveRepository.save(reserve);
    }
}
