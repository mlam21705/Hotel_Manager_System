package com.groupt1.hms.controller;

import com.groupt1.hms.dto.CustomerBookingRequest;
import com.groupt1.hms.dto.CustomerProfileRequest;
import com.groupt1.hms.entity.Guest;
import com.groupt1.hms.entity.Reserve;
import com.groupt1.hms.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping({"", "/home"})
    public String home(HttpSession session, Model model) {
        Integer userId = currentCustomerUserId(session);
        Guest guest = customerService.getGuestByUserId(userId);
        List<Reserve> reservations = customerService.getReservations(userId);
        model.addAttribute("guest", guest);
        model.addAttribute("recentReservations", reservations.stream().limit(5).toList());
        return "customer/home";
    }

    @GetMapping("/rooms")
    public String rooms(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                        @RequestParam(required = false) Integer roomTypeId,
                        @RequestParam(required = false) Integer capacity,
                        @RequestParam(required = false) Integer branchId,
                        HttpSession session,
                        Model model) {
        currentCustomerUserId(session);
        model.addAttribute("roomTypes", customerService.getRoomTypes());
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("capacity", capacity);
        model.addAttribute("branchId", branchId);
        if (checkIn != null || checkOut != null) {
            try {
                model.addAttribute("rooms", customerService.searchAvailableRooms(checkIn, checkOut, roomTypeId, capacity, branchId));
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
                model.addAttribute("rooms", customerService.getAllRooms());
            }
        } else {
            model.addAttribute("rooms", customerService.getAllRooms());
        }
        return "customer/search-rooms";
    }

    @GetMapping("/booking")
    public String bookingForm(@RequestParam Integer branchId,
                              @RequestParam String roomNo,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                              HttpSession session,
                              Model model) {
        currentCustomerUserId(session);
        CustomerBookingRequest request = new CustomerBookingRequest();
        request.setBranchId(branchId);
        request.setRoomNo(roomNo);
        request.setCheckInDate(checkIn);
        request.setCheckOutDate(checkOut);
        request.setNumberOfGuest(1);
        model.addAttribute("bookingRequest", request);
        model.addAttribute("room", customerService.getRoom(branchId, roomNo));
        model.addAttribute("services", customerService.getServices());
        return "customer/booking-form";
    }

    @PostMapping("/booking")
    public String createBooking(@ModelAttribute("bookingRequest") CustomerBookingRequest request,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Reserve reserve = customerService.createReservation(currentCustomerUserId(session), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt phòng thành công. Đơn đang chờ lễ tân xác nhận.");
            return "redirect:/customer/reservations/" + reserve.getReservationId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("room", customerService.getRoom(request.getBranchId(), request.getRoomNo()));
            model.addAttribute("services", customerService.getServices());
            return "customer/booking-form";
        } catch (Exception ex) {
            ex.printStackTrace(); // Log lỗi ra console để debug
            model.addAttribute("errorMessage", "Hệ thống gặp sự cố trong quá trình lưu dữ liệu: " + ex.getMessage());
            model.addAttribute("room", customerService.getRoom(request.getBranchId(), request.getRoomNo()));
            model.addAttribute("services", customerService.getServices());
            return "customer/booking-form";
        }
    }

    @GetMapping("/reservations")
    public String reservations(HttpSession session, Model model) {
        model.addAttribute("reservations", customerService.getReservations(currentCustomerUserId(session)));
        return "customer/reservation-history";
    }

    @GetMapping("/reservations/{id}")
    public String reservationDetail(@PathVariable Integer id, HttpSession session, Model model) {
        model.addAttribute("reservation", customerService.getReservation(currentCustomerUserId(session), id));
        return "customer/reservation-detail";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Integer id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            customerService.cancelReservation(currentCustomerUserId(session), id);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn đặt phòng thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/reservations/" + id;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Guest guest = customerService.getGuestByUserId(currentCustomerUserId(session));
        CustomerProfileRequest request = new CustomerProfileRequest();
        request.setGuestName(guest.getGuestName());
        request.setCccd(guest.getCccd());
        request.setPhoneNumber(guest.getPhoneNumber());
        request.setAddress(guest.getAddress());
        model.addAttribute("profile", request);
        model.addAttribute("guest", guest);
        return "customer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("profile") CustomerProfileRequest request,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Integer userId = currentCustomerUserId(session);
        try {
            customerService.updateProfile(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công.");
            return "redirect:/customer/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("guest", customerService.getGuestByUserId(userId));
            return "customer/profile";
        }
    }

    private Integer currentCustomerUserId(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof Map<?, ?> userMap) || userMap.get("userId") == null) {
            throw new IllegalStateException("Vui lòng đăng nhập trước.");
        }
        Object roleObj = userMap.get("role");
        if (roleObj == null || !("Guest".equalsIgnoreCase(roleObj.toString()) || "Customer".equalsIgnoreCase(roleObj.toString()))) {
            throw new IllegalStateException("Tài khoản hiện tại không có quyền Customer.");
        }
        Object rawId = userMap.get("userId");
        return rawId instanceof Integer ? (Integer) rawId : Integer.valueOf(rawId.toString());
    }
    @ExceptionHandler(IllegalStateException.class)
    public String handleCustomerAuthenticationException(
            IllegalStateException exception,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                exception.getMessage()
        );

        return "redirect:/login.html";
    }
}
