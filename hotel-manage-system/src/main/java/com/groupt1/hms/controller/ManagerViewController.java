package com.groupt1.hms.controller;

import com.groupt1.hms.entity.*;
import com.groupt1.hms.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/manager")
public class ManagerViewController {

    @Autowired
    private ManagerService managerService;

    // ──────────────── Dashboard ────────────────

    @GetMapping("/home")
    public String managerHome(Model model) {
        model.addAttribute("activePage", "home");
        return "manager/manager-home";
    }

    // ──────────────── Room Management ────────────────

    @GetMapping("/rooms")
    public String roomsView(
            @RequestParam(value = "branchId", required = false) Integer branchId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<Room> rooms;
        if (branchId != null) {
            rooms = managerService.getRoomsByBranch(branchId);
        } else {
            rooms = managerService.getAllRooms();
        }

        // Apply status filter if present
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            String finalStatus = status.trim();
            rooms = rooms.stream()
                    .filter(r -> finalStatus.equalsIgnoreCase(r.getRoomStatus()))
                    .toList();
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", managerService.getAllRoomTypes());
        model.addAttribute("selectedBranch", branchId != null ? branchId.toString() : "ALL");
        model.addAttribute("selectedStatus", status != null && !status.trim().isEmpty() ? status : "ALL");
        model.addAttribute("searchKeyword", search != null ? search : "");
        model.addAttribute("activePage", "rooms");

        return "manager/rooms";
    }

    @PostMapping("/rooms/save")
    public String saveRoom(
            @RequestParam("branchId") Integer branchId,
            @RequestParam("roomNo") String roomNo,
            @RequestParam("roomTypeId") Integer roomTypeId,
            @RequestParam("floor") Integer floor,
            @RequestParam("roomStatus") String roomStatus,
            @RequestParam(value = "isEdit", required = false, defaultValue = "false") boolean isEdit,
            RedirectAttributes redirectAttributes) {

        try {
            RoomId roomId = new RoomId(branchId, roomNo.trim());

            if (!isEdit) {
                Optional<Room> existing = managerService.getRoomById(roomId);
                if (existing.isPresent()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Room " + roomNo + " already exists in Branch " + branchId + ".");
                    return "redirect:/manager/rooms";
                }
            }

            RoomType roomType = managerService.getRoomTypeById(roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Room Category selected."));

            Room room = new Room();
            room.setId(roomId);
            room.setFloor(floor);
            room.setRoomStatus(roomStatus);
            room.setRoomType(roomType);

            managerService.saveRoom(room);
            redirectAttributes.addFlashAttribute("successMessage", "Room saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving room: " + e.getMessage());
        }

        return "redirect:/manager/rooms";
    }

    @PostMapping("/rooms/delete")
    public String deleteRoom(
            @RequestParam("branchId") Integer branchId,
            @RequestParam("roomNo") String roomNo,
            RedirectAttributes redirectAttributes) {

        try {
            RoomId roomId = new RoomId(branchId, roomNo.trim());
            managerService.deleteRoom(roomId);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting room: " + e.getMessage());
        }

        return "redirect:/manager/rooms";
    }

    // ──────────────── Room Categories (Types) ────────────────

    @GetMapping("/room-types")
    public String roomTypesView(Model model) {
        model.addAttribute("roomTypes", managerService.getAllRoomTypes());
        model.addAttribute("activePage", "room-types");
        return "manager/room-types";
    }

    @PostMapping("/room-types/save")
    public String saveRoomType(
            @RequestParam(value = "roomTypeId", required = false) Integer roomTypeId,
            @RequestParam("typeName") String typeName,
            @RequestParam("capacity") Integer capacity,
            @RequestParam("basePrice") BigDecimal basePrice,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam(value = "keepImageIds", required = false) List<Integer> keepImageIds,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            RedirectAttributes redirectAttributes) {

        try {
            if (capacity <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Capacity must be greater than 0.");
                return "redirect:/manager/room-types";
            }
            if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Base Price cannot be negative.");
                return "redirect:/manager/room-types";
            }

            RoomType roomType;
            if (roomTypeId != null) {
                roomType = managerService.getRoomTypeById(roomTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Room Type ID."));
                if (roomType.getImages() != null) {
                    roomType.getImages().removeIf(img -> keepImageIds == null || !keepImageIds.contains(img.getImageId()));
                }
            } else {
                roomType = new RoomType();
            }
            roomType.setTypeName(typeName.trim());
            roomType.setCapacity(capacity);
            roomType.setBasePrice(basePrice);
            roomType.setDescription(description.trim());

            RoomType savedRoomType = managerService.saveRoomType(roomType);

            // Determine if there's any remaining primary image
            boolean hasPrimary = false;
            if (savedRoomType.getImages() != null) {
                for (RoomImage img : savedRoomType.getImages()) {
                    if (Boolean.TRUE.equals(img.getIsPrimary())) {
                        hasPrimary = true;
                    }
                }
            }

            // Save new uploaded files
            List<String> uploadedPaths = saveUploadedFiles(imageFiles);
            for (String path : uploadedPaths) {
                RoomImage roomImage = new RoomImage();
                roomImage.setRoomType(savedRoomType);
                roomImage.setImageUrl(path);
                roomImage.setIsPrimary(!hasPrimary);
                hasPrimary = true;
                managerService.saveRoomImage(roomImage);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Room Type saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }

        return "redirect:/manager/room-types";
    }

    private List<String> saveUploadedFiles(MultipartFile[] files) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        if (files == null || files.length == 0) {
            return savedPaths;
        }

        // Determine upload directory
        File uploadDir = new File("../free-hotel-website-template/uploads").getAbsoluteFile();
        if (!uploadDir.getParentFile().exists()) {
            uploadDir = new File("free-hotel-website-template/uploads").getAbsoluteFile();
        }
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String originalName = file.getOriginalFilename();
                String ext = "";
                if (originalName != null && originalName.contains(".")) {
                    ext = originalName.substring(originalName.lastIndexOf("."));
                }
                String newFileName = UUID.randomUUID().toString() + ext;
                File destFile = new File(uploadDir, newFileName).getAbsoluteFile();
                java.nio.file.Files.copy(file.getInputStream(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                savedPaths.add("/uploads/" + newFileName);
            }
        }
        return savedPaths;
    }

    @PostMapping("/room-types/delete")
    public String deleteRoomType(
            @RequestParam("roomTypeId") Integer roomTypeId,
            RedirectAttributes redirectAttributes) {

        try {
            managerService.deleteRoomType(roomTypeId);
            redirectAttributes.addFlashAttribute("successMessage", "Room Type deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }

        return "redirect:/manager/room-types";
    }

    @GetMapping("/room-types/view/{id}")
    public String viewRoomTypeDetails(@PathVariable("id") Integer id, Model model) {
        RoomType roomType = managerService.getRoomTypeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Room Type ID: " + id));
        model.addAttribute("roomType", roomType);
        model.addAttribute("activePage", "room-types");
        return "manager/room-type-details";
    }

    // ──────────────── Hotel Services ────────────────

    @GetMapping("/services")
    public String servicesView(Model model) {
        model.addAttribute("services", managerService.getAllServices());
        model.addAttribute("activePage", "services");
        return "manager/services";
    }

    @PostMapping("/services/save")
    public String saveService(
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam("serviceName") String serviceName,
            @RequestParam("servicePrice") BigDecimal servicePrice,
            @RequestParam(value = "keepImageIds", required = false) List<Integer> keepImageIds,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            RedirectAttributes redirectAttributes) {

        try {
            if (servicePrice.compareTo(BigDecimal.ZERO) < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Service Price cannot be negative.");
                return "redirect:/manager/services";
            }

            Service service;
            if (serviceId != null) {
                service = managerService.getServiceById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Service ID."));
                if (service.getImages() != null) {
                    service.getImages().removeIf(img -> keepImageIds == null || !keepImageIds.contains(img.getImageId()));
                }
            } else {
                service = new Service();
            }
            service.setServiceName(serviceName.trim());
            service.setServicePrice(servicePrice);

            Service savedService = managerService.saveService(service);

            // Determine if there's any remaining primary image
            boolean hasPrimary = false;
            if (savedService.getImages() != null) {
                for (RoomImage img : savedService.getImages()) {
                    if (Boolean.TRUE.equals(img.getIsPrimary())) {
                        hasPrimary = true;
                    }
                }
            }

            // Save new uploaded files
            List<String> uploadedPaths = saveUploadedFiles(imageFiles);
            for (String path : uploadedPaths) {
                RoomImage roomImage = new RoomImage();
                roomImage.setService(savedService);
                roomImage.setImageUrl(path);
                roomImage.setIsPrimary(!hasPrimary);
                hasPrimary = true;
                managerService.saveRoomImage(roomImage);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Service saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }

        return "redirect:/manager/services";
    }

    @PostMapping("/services/delete")
    public String deleteService(
            @RequestParam("serviceId") Integer serviceId,
            RedirectAttributes redirectAttributes) {

        try {
            managerService.deleteService(serviceId);
            redirectAttributes.addFlashAttribute("successMessage", "Service deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }

        return "redirect:/manager/services";
    }

    @GetMapping("/services/view/{id}")
    public String viewServiceDetails(@PathVariable("id") Integer id, Model model) {
        Service service = managerService.getServiceById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Service ID: " + id));
        model.addAttribute("service", service);
        model.addAttribute("activePage", "services");
        return "manager/service-details";
    }

    // ──────────────── Business Reports ────────────────

    @GetMapping("/reports")
    public String reportsView(
            @RequestParam(value = "branchId", required = false, defaultValue = "0") Integer branchId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            Model model) {

        // Default to current year and month if not provided
        LocalDate now = LocalDate.now();
        int finalYear = year != null ? year : now.getYear();
        int finalMonth = month != null ? month : now.getMonthValue();

        Map<String, Object> occupancyReport = managerService.getBranchOccupancyReport(branchId);
        Map<String, Object> revenueReport = managerService.getRevenueReport(branchId, finalYear, finalMonth);

        model.addAttribute("occupancyReport", occupancyReport);
        model.addAttribute("revenueReport", revenueReport);
        model.addAttribute("selectedBranch", branchId);
        model.addAttribute("selectedYear", finalYear);
        model.addAttribute("selectedMonth", finalMonth);
        model.addAttribute("activePage", "reports");

        return "manager/reports";
    }
}
