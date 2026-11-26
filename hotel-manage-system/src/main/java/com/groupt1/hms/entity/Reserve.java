package com.groupt1.hms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Reserve")
public class Reserve {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReservationID")
    private Integer reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GuestID", nullable = false)
    private Guest guest;

    @CreationTimestamp
    @Column(name = "ReserveDate", updatable = false)
    private LocalDateTime reserveDate;

    @Column(name = "ReservationStatus", length = 50)
    private String reservationStatus = "Pending";

    @Column(name = "TotalAmount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "reserve", cascade = CascadeType.ALL)
    private List<ReserveDetail> reserveDetails;

    @OneToOne(mappedBy = "reserve", cascade = CascadeType.ALL)
    private Invoice invoice;

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public LocalDateTime getReserveDate() {
        return reserveDate;
    }

    public void setReserveDate(LocalDateTime reserveDate) {
        this.reserveDate = reserveDate;
    }

    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<ReserveDetail> getReserveDetails() {
        return reserveDetails;
    }

    public void setReserveDetails(List<ReserveDetail> reserveDetails) {
        this.reserveDetails = reserveDetails;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }
}
