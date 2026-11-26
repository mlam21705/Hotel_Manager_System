package com.groupt1.hms.service;

import com.groupt1.hms.entity.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ManagerService {
    // ──────────────── Room Types ────────────────
    List<RoomType> getAllRoomTypes();
    Optional<RoomType> getRoomTypeById(Integer id);
    RoomType saveRoomType(RoomType roomType);
    void deleteRoomType(Integer id);

    // ──────────────── Rooms ────────────────
    List<Room> getAllRooms();
    List<Room> getRoomsByBranch(Integer branchId);
    Optional<Room> getRoomById(RoomId roomId);
    Room saveRoom(Room room);
    void deleteRoom(RoomId roomId);

    // ──────────────── Room Images ────────────────
    List<RoomImage> getImagesByRoom(RoomId roomId);
    List<RoomImage> getImagesByRoomType(Integer roomTypeId);
    List<RoomImage> getImagesByService(Integer serviceId);
    RoomImage saveRoomImage(RoomImage roomImage);
    void deleteRoomImage(Integer imageId);

    // ──────────────── Services ────────────────
    List<Service> getAllServices();
    Optional<Service> getServiceById(Integer id);
    Service saveService(Service service);
    void deleteService(Integer id);

    // ──────────────── Reports ────────────────
    Map<String, Object> getBranchOccupancyReport(Integer branchId);
    Map<String, Object> getRevenueReport(Integer branchId, Integer year, Integer month);
}
