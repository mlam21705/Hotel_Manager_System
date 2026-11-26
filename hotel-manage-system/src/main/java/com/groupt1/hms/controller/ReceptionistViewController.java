package com.groupt1.hms.controller;

import com.groupt1.hms.dto.WalkInBookingRequest;
import com.groupt1.hms.entity.*;
import com.groupt1.hms.service.ManagerService;
import com.groupt1.hms.service.ReceptionistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistViewController {

    @Autowired
    private ReceptionistService receptionistService;

    @Autowired
    private ManagerService managerService;

    // ──────────────── Redirect to Home ────────────────
    @GetMapping
    public String index() {
        return "redirect:/receptionist/home";
    }

    // ──────────────── Dashboard ────────────────
    @GetMapping("/home")
    public String home(Model model) {
        List<Room> allRooms = receptionistService.getRoomGrid(null, null);
        long emptyCount = allRooms.stream().filter(r -> "Trống".equalsIgnoreCase(r.getRoomStatus())).count();
        long occupiedCount = allRooms.stream().filter(r -> "Đang sử dụng".equalsIgnoreCase(r.getRoomStatus())).count();
        long cleaningCount = allRooms.stream().filter(r -> "Đang dọn".equalsIgnoreCase(r.getRoomStatus())).count();
        long maintenanceCount = allRooms.stream().filter(r -> "Đang bảo trì".equalsIgnoreCase(r.getRoomStatus())).count();

        List<Reserve> pendingReservations = receptionistService.searchReservations(null, null, "Pending");

        model.addAttribute("emptyCount", emptyCount);
        model.addAttribute("occupiedCount", occupiedCount);
        model.addAttribute("cleaningCount", cleaningCount);
        model.addAttribute("maintenanceCount", maintenanceCount);
        model.addAttribute("pendingCount", pendingReservations.size());
        model.addAttribute("pendingReservations", pendingReservations);
        model.addAttribute("activePage", "home");

        return "receptionist/receptionist-home";
    }

    // ──────────────── Room Grid (RCPT-01) ────────────────
    @GetMapping("/rooms")
    public String roomGrid(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        List<Room> rooms = receptionistService.getRoomGrid(branchId, status);

        // Group rooms by branch ID
        Map<Integer, List<Room>> roomsGroupedByBranch = rooms.stream()
                .collect(Collectors.groupingBy(r -> r.getId().getBranchId()));

        model.addAttribute("roomsGroupedByBranch", roomsGroupedByBranch);
        model.addAttribute("selectedBranch", branchId != null ? branchId.toString() : "ALL");
        model.addAttribute("selectedStatus", status != null && !status.trim().isEmpty() ? status : "ALL");
        model.addAttribute("activePage", "rooms");

        return "receptionist/room-grid";
    }

    @PostMapping("/rooms/finish-cleaning")
    public String finishCleaning(
            @RequestParam("branchId") Integer branchId,
            @RequestParam("roomNo") String roomNo,
            RedirectAttributes redirectAttributes) {
        try {
            receptionistService.finishCleaning(branchId, roomNo);
            redirectAttributes.addFlashAttribute("successMessage", "Đã dọn dẹp xong phòng " + roomNo + ". Phòng hiện đã trống và sẵn sàng sử dụng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/receptionist/rooms";
    }

    @GetMapping("/rooms/stay")
    public String viewStayDetails(
            @RequestParam("branchId") Integer branchId,
            @RequestParam("roomNo") String roomNo,
            RedirectAttributes redirectAttributes) {
        Optional<Reserve> activeRes = receptionistService.getActiveReservationByRoom(branchId, roomNo);
        if (activeRes.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy lượt lưu trú hoạt động nào cho phòng " + roomNo + ".");
            return "redirect:/receptionist/rooms";
        }
        return "redirect:/receptionist/reservations/" + activeRes.get().getReservationId();
    }

    // ──────────────── Reservations Search (RCPT-02) ────────────────
    @GetMapping("/reservations")
    public String searchReservations(
            @RequestParam(value = "guestName", required = false) String guestName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        List<Reserve> reservations = receptionistService.searchReservations(guestName, phone, status);

        model.addAttribute("reservations", reservations);
        model.addAttribute("guestName", guestName != null ? guestName : "");
        model.addAttribute("phone", phone != null ? phone : "");
        model.addAttribute("selectedStatus", status != null && !status.trim().isEmpty() ? status : "ALL");
        model.addAttribute("activePage", "reservations");

        return "receptionist/reservations";
    }

    // ──────────────── Confirm Reservation (RCPT-03) ────────────────
    @PostMapping("/reservations/confirm")
    public String confirmReservation(
            @RequestParam("reservationId") Integer reservationId,
            @RequestParam(value = "depositConfirmed", required = false, defaultValue = "false") boolean depositConfirmed,
            RedirectAttributes redirectAttributes) {

        try {
            if (!depositConfirmed) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng xác nhận khách đã đặt cọc trước khi xác nhận đơn.");
                return "redirect:/receptionist/reservations";
            }

            receptionistService.confirmReservation(reservationId);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đặt phòng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/receptionist/reservations";
    }

    // ──────────────── Reservation Detail ────────────────
    @GetMapping("/reservations/{id}")
    public String reservationDetail(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Reserve> optReserve = receptionistService.getReservationById(id);
        if (optReserve.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn đặt phòng không tồn tại.");
            return "redirect:/receptionist/reservations";
        }

        Reserve reserve = optReserve.get();
        List<Service> services = receptionistService.getAllServices();

        model.addAttribute("reserve", reserve);
        model.addAttribute("services", services);
        model.addAttribute("activePage", "reservations");

        return "receptionist/reservation-detail";
    }

    // ──────────────── Check-In (RCPT-04) ────────────────
    @PostMapping("/checkin")
    public String checkIn(
            @RequestParam("reservationId") Integer reservationId,
            @RequestParam("reserveDetailId") Integer reserveDetailId,
            RedirectAttributes redirectAttributes) {

        try {
            receptionistService.checkIn(reservationId, reserveDetailId);
            redirectAttributes.addFlashAttribute("successMessage", "Nhận phòng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi nhận phòng: " + e.getMessage());
        }

        return "redirect:/receptionist/reservations/" + reservationId;
    }

    // ──────────────── Checkout Preview (RCPT-05) ────────────────
    @GetMapping("/checkout/{reserveDetailId}")
    public String checkoutPreview(@PathVariable("reserveDetailId") Integer reserveDetailId, Model model, RedirectAttributes redirectAttributes) {
        Optional<ReserveDetail> optDetail = receptionistService.getReserveDetailById(reserveDetailId);
        if (optDetail.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chi tiết phòng không tồn tại.");
            return "redirect:/receptionist/reservations";
        }

        ReserveDetail detail = optDetail.get();
        Reserve reserve = detail.getReserve();

        // Calculate nights and prices
        long nights = ChronoUnit.DAYS.between(detail.getCheckInDate(), detail.getCheckOutDate());
        if (nights <= 0) nights = 1;

        BigDecimal roomCost = detail.getPrice().multiply(BigDecimal.valueOf(nights));
        List<UseService> servicesUsed = detail.getUseServices();
        BigDecimal serviceCost = BigDecimal.ZERO;
        for (UseService us : servicesUsed) {
            serviceCost = serviceCost.add(us.getPriceAtTime().multiply(BigDecimal.valueOf(us.getQuantity())));
        }

        BigDecimal totalAmount = roomCost.add(serviceCost);

        model.addAttribute("detail", detail);
        model.addAttribute("reserve", reserve);
        model.addAttribute("nights", nights);
        model.addAttribute("roomCost", roomCost);
        model.addAttribute("serviceCost", serviceCost);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("servicesUsed", servicesUsed);
        model.addAttribute("activePage", "reservations");

        return "receptionist/checkout";
    }

    // ──────────────── Confirm Checkout (RCPT-05) ────────────────
    @PostMapping("/checkout")
    public String confirmCheckout(
            @RequestParam("reserveDetailId") Integer reserveDetailId,
            RedirectAttributes redirectAttributes) {

        try {
            Invoice invoice = receptionistService.checkOut(reserveDetailId);
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán và trả phòng thành công.");
            return "redirect:/receptionist/invoice/" + invoice.getInvoiceId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi trả phòng: " + e.getMessage());
            return "redirect:/receptionist/reservations";
        }
    }

    // ──────────────── Print Invoice ────────────────
    @GetMapping("/invoice/{invoiceId}")
    public String viewInvoice(@PathVariable("invoiceId") Integer invoiceId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Invoice> optInvoice = receptionistService.getInvoiceById(invoiceId);
        if (optInvoice.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hoá đơn không tồn tại.");
            return "redirect:/receptionist/reservations";
        }

        Invoice invoice = optInvoice.get();
        Reserve reserve = invoice.getReserve();
        List<ReserveDetail> details = reserve.getReserveDetails();

        // Calculate nights and breakdown for each room
        List<Map<String, Object>> roomBreakdowns = new ArrayList<>();
        BigDecimal totalRoomCost = BigDecimal.ZERO;
        BigDecimal totalServiceCost = BigDecimal.ZERO;

        for (ReserveDetail d : details) {
            long nights = ChronoUnit.DAYS.between(d.getCheckInDate(), d.getCheckOutDate());
            if (nights <= 0) nights = 1;

            BigDecimal roomCost = d.getPrice().multiply(BigDecimal.valueOf(nights));
            totalRoomCost = totalRoomCost.add(roomCost);

            BigDecimal detailServiceCost = BigDecimal.ZERO;
            if (d.getUseServices() != null) {
                for (UseService us : d.getUseServices()) {
                    detailServiceCost = detailServiceCost.add(us.getPriceAtTime().multiply(BigDecimal.valueOf(us.getQuantity())));
                }
            }
            totalServiceCost = totalServiceCost.add(detailServiceCost);

            Map<String, Object> map = new HashMap<>();
            map.put("roomNo", d.getRoom().getId().getRoomNo());
            map.put("branchId", d.getRoom().getId().getBranchId());
            map.put("typeName", d.getRoom().getRoomType().getTypeName());
            map.put("price", d.getPrice());
            map.put("nights", nights);
            map.put("checkIn", d.getCheckInDate());
            map.put("checkOut", d.getCheckOutDate());
            map.put("roomCost", roomCost);
            map.put("serviceCost", detailServiceCost);
            map.put("total", roomCost.add(detailServiceCost));
            roomBreakdowns.add(map);
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("reserve", reserve);
        model.addAttribute("roomBreakdowns", roomBreakdowns);
        model.addAttribute("totalRoomCost", totalRoomCost);
        model.addAttribute("totalServiceCost", totalServiceCost);
        model.addAttribute("grandTotal", invoice.getTotalAmount());
        model.addAttribute("activePage", "reservations");

        return "receptionist/invoice";
    }

    // ──────────────── Walk-In Form (RCPT-06) ────────────────
    @GetMapping("/walkin")
    public String walkInForm(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "roomNo", required = false) String roomNo,
            Model model) {
        List<Room> vacantRooms = receptionistService.getRoomGrid(null, "Trống");

        model.addAttribute("vacantRooms", vacantRooms);
        model.addAttribute("preselectedBranchId", branchId);
        model.addAttribute("preselectedRoomNo", roomNo);
        model.addAttribute("activePage", "walkin");

        return "receptionist/walkin-form";
    }

    // ──────────────── Walk-In Save (RCPT-06) ────────────────
    @PostMapping("/walkin/save")
    public String saveWalkIn(
            @RequestParam("guestName") String guestName,
            @RequestParam("cccd") String cccd,
            @RequestParam("phone") String phone,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam("branchId") Integer branchId,
            @RequestParam("roomNo") String roomNo,
            @RequestParam("checkInDate") String checkInDateStr,
            @RequestParam("checkOutDate") String checkOutDateStr,
            @RequestParam("numberOfGuest") Integer numberOfGuest,
            RedirectAttributes redirectAttributes) {

        try {
            LocalDate checkInDate = LocalDate.parse(checkInDateStr);
            LocalDate checkOutDate = LocalDate.parse(checkOutDateStr);

            WalkInBookingRequest request = WalkInBookingRequest.builder()
                    .guestName(guestName)
                    .cccd(cccd)
                    .phone(phone)
                    .address(address)
                    .branchId(branchId)
                    .roomNo(roomNo)
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .numberOfGuest(numberOfGuest)
                    .build();

            Reserve reserve = receptionistService.createWalkInReservation(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt phòng Walk-in thành công. Số đơn: " + reserve.getReservationId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo đặt phòng Walk-in: " + e.getMessage());
            return "redirect:/receptionist/walkin";
        }

        return "redirect:/receptionist/reservations";
    }

    // ──────────────── Add Service to Stay ────────────────
    @PostMapping("/reservations/{reserveDetailId}/services/add")
    public String addService(
            @PathVariable("reserveDetailId") Integer reserveDetailId,
            @RequestParam("reservationId") Integer reservationId,
            @RequestParam("serviceId") Integer serviceId,
            @RequestParam("quantity") Integer quantity,
            RedirectAttributes redirectAttributes) {

        try {
            receptionistService.addServiceToStay(reserveDetailId, serviceId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm dịch vụ thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thêm dịch vụ: " + e.getMessage());
        }

        return "redirect:/receptionist/reservations/" + reservationId;
    }

    // ──────────────── Extend Room Stay ────────────────
    @PostMapping("/reservations/{reserveDetailId}/extend")
    public String extendRoom(
            @PathVariable("reserveDetailId") Integer reserveDetailId,
            @RequestParam("reservationId") Integer reservationId,
            @RequestParam("newCheckOutDate") String newCheckOutDateStr,
            RedirectAttributes redirectAttributes) {

        try {
            LocalDate newCheckOutDate = LocalDate.parse(newCheckOutDateStr);
            receptionistService.extendRoom(reserveDetailId, newCheckOutDate);
            redirectAttributes.addFlashAttribute("successMessage", "Gia hạn phòng thành công đến ngày " + newCheckOutDateStr + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gia hạn phòng: " + e.getMessage());
        }

        return "redirect:/receptionist/reservations/" + reservationId;
    }
}
