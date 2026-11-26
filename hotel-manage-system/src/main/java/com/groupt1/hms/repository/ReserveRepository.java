package com.groupt1.hms.repository;

import com.groupt1.hms.entity.Reserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReserveRepository extends JpaRepository<Reserve, Integer> {

    @Query("SELECT r FROM Reserve r JOIN r.guest g WHERE " +
           "(:guestName IS NULL OR :guestName = '' OR LOWER(g.guestName) LIKE LOWER(CONCAT('%', :guestName, '%'))) AND " +
           "(:phone IS NULL OR :phone = '' OR g.phoneNumber LIKE CONCAT('%', :phone, '%')) AND " +
           "(:status IS NULL OR :status = '' OR r.reservationStatus = :status) " +
           "ORDER BY r.reserveDate DESC")
    List<Reserve> searchReservations(@Param("guestName") String guestName,
                                     @Param("phone") String phone,
                                     @Param("status") String status);

    @Query("SELECT r FROM Reserve r JOIN r.reserveDetails rd WHERE " +
           "rd.room.id.branchId = :branchId AND rd.room.id.roomNo = :roomNo " +
           "AND r.reservationStatus = 'Checked-In'")
    List<Reserve> findCheckedInReservationsByRoom(@Param("branchId") Integer branchId, @Param("roomNo") String roomNo);
}
