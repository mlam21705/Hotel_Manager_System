package com.groupt1.hms.repository;

import com.groupt1.hms.entity.ReserveDetail;
import com.groupt1.hms.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReserveDetailRepository extends JpaRepository<ReserveDetail, Integer> {
    List<ReserveDetail> findByReserve_ReservationId(Integer reservationId);
    List<ReserveDetail> findByRoom_Id(RoomId roomId);
    void deleteByRoom_Id(RoomId roomId);
}
