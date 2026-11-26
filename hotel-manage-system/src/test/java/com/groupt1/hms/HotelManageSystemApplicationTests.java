package com.groupt1.hms;

import com.groupt1.hms.entity.*;
import com.groupt1.hms.repository.*;
import com.groupt1.hms.service.ManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class HotelManageSystemApplicationTests {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private ManagerService managerService;

    @Test
    void contextLoads() {
        System.out.println("====== DB ROOM TYPES ======");
        List<RoomType> types = roomTypeRepository.findAll();
        for (RoomType t : types) {
            System.out.println("RoomType ID: " + t.getRoomTypeId() + ", Name: " + t.getTypeName() + ", BasePrice: " + t.getBasePrice());
        }

        System.out.println("====== DB ROOMS ======");
        List<Room> rooms = roomRepository.findAll();
        for (Room r : rooms) {
            System.out.println("Room: Branch " + r.getId().getBranchId() + ", RoomNo " + r.getId().getRoomNo() + ", TypeID: " + r.getRoomType().getRoomTypeId() + ", Status: " + r.getRoomStatus());
        }
        System.out.println("======================");
    }

    @Test
    void testEditRoom() {
        RoomId id = new RoomId(1, "102"); // existing room in Branch 1
        Room room = new Room();
        room.setId(id);
        room.setFloor(2); // change floor
        room.setRoomStatus("Trống");
        room.setRoomType(managerService.getRoomTypeById(2).get());
        
        System.out.println("====== ATTEMPTING TO EDIT ROOM ======");
        try {
            managerService.saveRoom(room);
            System.out.println("EDIT ROOM SUCCESSFUL!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EDIT ROOM FAILED: " + e.getMessage());
        }
        System.out.println("=====================================");
    }
}
