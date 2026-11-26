package com.groupt1.hms.repository;

import com.groupt1.hms.entity.UseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UseServiceRepository extends JpaRepository<UseService, Integer> {
    List<UseService> findByReserveDetail_ReserveDetailId(Integer reserveDetailId);
    void deleteByService_ServiceId(Integer serviceId);
}
