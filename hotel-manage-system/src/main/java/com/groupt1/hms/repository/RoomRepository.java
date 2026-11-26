package com.groupt1.hms.repository;

import com.groupt1.hms.entity.Room;
import com.groupt1.hms.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, RoomId> {
    List<Room> findById_BranchId(Integer branchId);
    boolean existsByRoomType_RoomTypeId(Integer roomTypeId);
    List<Room> findByRoomType_RoomTypeId(Integer roomTypeId);
}
