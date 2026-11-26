package com.groupt1.hms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "UseService")
public class UseService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UseID")
    private Integer useId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReserveDetailID", nullable = false)
    private ReserveDetail reserveDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServiceID", nullable = false)
    private Service service;

    @CreationTimestamp
    @Column(name = "UseDate", updatable = false)
    private LocalDateTime useDate;

    @Column(name = "Quantity")
    private Integer quantity = 1;

    @Column(name = "PriceAtTime", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtTime;

    public Integer getUseId() {
        return useId;
    }

    public void setUseId(Integer useId) {
        this.useId = useId;
    }

    public ReserveDetail getReserveDetail() {
        return reserveDetail;
    }

    public void setReserveDetail(ReserveDetail reserveDetail) {
        this.reserveDetail = reserveDetail;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public LocalDateTime getUseDate() {
        return useDate;
    }

    public void setUseDate(LocalDateTime useDate) {
        this.useDate = useDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtTime() {
        return priceAtTime;
    }

    public void setPriceAtTime(BigDecimal priceAtTime) {
        this.priceAtTime = priceAtTime;
    }
}
