package com.groupt1.hms.repository;

import com.groupt1.hms.entity.Room;
import com.groupt1.hms.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CustomerRoomRepository extends JpaRepository<Room, RoomId> {

    @Query("""
        SELECT DISTINCT r
        FROM Room r
        JOIN FETCH r.roomType rt
        WHERE LOWER(r.roomStatus) IN ('trống', 'ready', 'vacant', 'available')
          AND (:roomTypeId IS NULL OR rt.roomTypeId = :roomTypeId)
          AND (:capacity IS NULL OR rt.capacity >= :capacity)
          AND (:branchId IS NULL OR r.id.branchId = :branchId)
          AND NOT EXISTS (
              SELECT rd.reserveDetailId
              FROM ReserveDetail rd
              WHERE rd.room = r
                AND LOWER(rd.reserve.reservationStatus) NOT IN ('cancelled', 'checked-out')
                AND rd.checkInDate < :checkOut
                AND rd.checkOutDate > :checkIn
          )
        ORDER BY r.id.branchId, r.id.roomNo
        """)
    List<Room> searchAvailableRooms(@Param("checkIn") LocalDate checkIn,
                                    @Param("checkOut") LocalDate checkOut,
                                    @Param("roomTypeId") Integer roomTypeId,
                                    @Param("capacity") Integer capacity,
                                    @Param("branchId") Integer branchId);

    @Query("""
        SELECT CASE WHEN COUNT(rd) = 0 THEN true ELSE false END
        FROM ReserveDetail rd
        WHERE rd.room.id.branchId = :branchId
          AND rd.room.id.roomNo = :roomNo
          AND LOWER(rd.reserve.reservationStatus) NOT IN ('cancelled', 'checked-out')
          AND rd.checkInDate < :checkOut
          AND rd.checkOutDate > :checkIn
        """)
    boolean isRoomAvailable(@Param("branchId") Integer branchId,
                            @Param("roomNo") String roomNo,
                            @Param("checkIn") LocalDate checkIn,
                            @Param("checkOut") LocalDate checkOut);
}
