package com.groupt1.hms.repository;

import com.groupt1.hms.entity.RoomImage;
import com.groupt1.hms.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Integer> {
    List<RoomImage> findByRoom_Id(RoomId roomId);
    List<RoomImage> findByRoomType_RoomTypeId(Integer roomTypeId);
    List<RoomImage> findByService_ServiceId(Integer serviceId);
    void deleteByRoomType_RoomTypeId(Integer roomTypeId);
    void deleteByService_ServiceId(Integer serviceId);
    void deleteByRoom_Id(RoomId roomId);
}
