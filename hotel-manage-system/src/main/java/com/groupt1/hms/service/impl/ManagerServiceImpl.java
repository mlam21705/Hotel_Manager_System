package com.groupt1.hms.service.impl;

import com.groupt1.hms.entity.*;
import com.groupt1.hms.repository.*;
import com.groupt1.hms.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ManagerServiceImpl implements ManagerService {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomImageRepository roomImageRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UseServiceRepository useServiceRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private MaintenanceReportRepository maintenanceReportRepository;

    @Autowired
    private ReserveDetailRepository reserveDetailRepository;

    // ──────────────── Room Types ────────────────

    @Override
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    @Override
    public Optional<RoomType> getRoomTypeById(Integer id) {
        return roomTypeRepository.findById(id);
    }

    @Override
    @Transactional
    public RoomType saveRoomType(RoomType roomType) {
        return roomTypeRepository.save(roomType);
    }

    @Override
    @Transactional
    public void deleteRoomType(Integer id) {
        roomImageRepository.deleteByRoomType_RoomTypeId(id);
        List<Room> rooms = roomRepository.findByRoomType_RoomTypeId(id);
        for (Room room : rooms) {
            deleteRoom(room.getId());
        }
        roomTypeRepository.deleteById(id);
    }

    // ──────────────── Rooms ────────────────

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public List<Room> getRoomsByBranch(Integer branchId) {
        return roomRepository.findById_BranchId(branchId);
    }

    @Override
    public Optional<Room> getRoomById(RoomId roomId) {
        return roomRepository.findById(roomId);
    }

    @Override
    @Transactional
    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public void deleteRoom(RoomId roomId) {
        roomImageRepository.deleteByRoom_Id(roomId);
        cleaningTaskRepository.deleteByRoom_Id(roomId);
        maintenanceReportRepository.deleteByRoom_Id(roomId);
        reserveDetailRepository.deleteByRoom_Id(roomId);
        roomRepository.deleteById(roomId);
    }

    // ──────────────── Room Images ────────────────

    @Override
    public List<RoomImage> getImagesByRoom(RoomId roomId) {
        return roomImageRepository.findByRoom_Id(roomId);
    }

    @Override
    public List<RoomImage> getImagesByRoomType(Integer roomTypeId) {
        return roomImageRepository.findByRoomType_RoomTypeId(roomTypeId);
    }

    @Override
    public List<RoomImage> getImagesByService(Integer serviceId) {
        return roomImageRepository.findByService_ServiceId(serviceId);
    }

    @Override
    @Transactional
    public RoomImage saveRoomImage(RoomImage roomImage) {
        return roomImageRepository.save(roomImage);
    }

    @Override
    @Transactional
    public void deleteRoomImage(Integer imageId) {
        roomImageRepository.deleteById(imageId);
    }

    // ──────────────── Services ────────────────

    @Override
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    public Optional<Service> getServiceById(Integer id) {
        return serviceRepository.findById(id);
    }

    @Override
    @Transactional
    public Service saveService(Service service) {
        return serviceRepository.save(service);
    }

    @Override
    @Transactional
    public void deleteService(Integer id) {
        roomImageRepository.deleteByService_ServiceId(id);
        useServiceRepository.deleteByService_ServiceId(id);
        serviceRepository.deleteById(id);
    }

    // ──────────────── Reports ────────────────

    @Override
    public Map<String, Object> getBranchOccupancyReport(Integer branchId) {
        List<Room> rooms;
        if (branchId == null || branchId == 0) {
            rooms = roomRepository.findAll();
        } else {
            rooms = roomRepository.findById_BranchId(branchId);
        }
        long totalRooms = rooms.size();
        long occupiedRooms = rooms.stream().filter(r -> "Đang sử dụng".equalsIgnoreCase(r.getRoomStatus())).count();
        long cleaningRooms = rooms.stream().filter(r -> "Đang dọn".equalsIgnoreCase(r.getRoomStatus())).count();
        long emptyRooms = rooms.stream().filter(r -> "Trống".equalsIgnoreCase(r.getRoomStatus())).count();

        double occupancyRate = totalRooms > 0 ? (occupiedRooms * 100.0) / totalRooms : 0.0;

        Map<String, Object> report = new HashMap<>();
        report.put("branchId", branchId);
        report.put("totalRooms", totalRooms);
        report.put("occupiedRooms", occupiedRooms);
        report.put("cleaningRooms", cleaningRooms);
        report.put("emptyRooms", emptyRooms);
        report.put("availableRooms", emptyRooms);
        report.put("occupancyRate", occupancyRate);
        return report;
    }

    @Override
    public Map<String, Object> getRevenueReport(Integer branchId, Integer year, Integer month) {
        BigDecimal totalRevenue;
        if (branchId == null || branchId == 0) {
            totalRevenue = invoiceRepository.sumRevenueAllBranchesAndMonth(year, month);
        } else {
            totalRevenue = invoiceRepository.sumRevenueByBranchAndMonth(branchId, year, month);
        }
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("branchId", branchId);
        report.put("year", year);
        report.put("month", month);
        report.put("totalRevenue", totalRevenue);
        return report;
    }
}
