package com.groupt1.hms.repository;

import com.groupt1.hms.entity.Reserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerReserveRepository extends JpaRepository<Reserve, Integer> {

    @Query("""
        SELECT r
        FROM Reserve r
        WHERE r.guest.guestId = :guestId
        ORDER BY r.reserveDate DESC
        """)
    List<Reserve> findByGuestIdOrderByReserveDateDesc(@Param("guestId") Integer guestId);

    @Query("""
        SELECT DISTINCT r
        FROM Reserve r
        LEFT JOIN FETCH r.reserveDetails rd
        LEFT JOIN FETCH rd.room room
        LEFT JOIN FETCH room.roomType
        WHERE r.reservationId = :reservationId
          AND r.guest.guestId = :guestId
        """)
    Optional<Reserve> findOwnedReservationWithDetails(@Param("reservationId") Integer reservationId,
                                                       @Param("guestId") Integer guestId);
}
