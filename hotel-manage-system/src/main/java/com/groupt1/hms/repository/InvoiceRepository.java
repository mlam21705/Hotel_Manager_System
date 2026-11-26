package com.groupt1.hms.repository;

import com.groupt1.hms.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    Optional<Invoice> findByReserve_ReservationId(Integer reservationId);

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i " +
           "JOIN i.reserve r " +
           "JOIN r.reserveDetails rd " +
           "WHERE rd.room.id.branchId = :branchId " +
           "AND i.invoiceStatus = 'Đã thanh toán' " +
           "AND FUNCTION('YEAR', i.invoiceDate) = :year " +
           "AND FUNCTION('MONTH', i.invoiceDate) = :month")
    BigDecimal sumRevenueByBranchAndMonth(@Param("branchId") Integer branchId,
                                         @Param("year") Integer year,
                                         @Param("month") Integer month);

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i " +
           "WHERE i.invoiceStatus = 'Đã thanh toán' " +
           "AND FUNCTION('YEAR', i.invoiceDate) = :year " +
           "AND FUNCTION('MONTH', i.invoiceDate) = :month")
    BigDecimal sumRevenueAllBranchesAndMonth(@Param("year") Integer year,
                                             @Param("month") Integer month);
}
