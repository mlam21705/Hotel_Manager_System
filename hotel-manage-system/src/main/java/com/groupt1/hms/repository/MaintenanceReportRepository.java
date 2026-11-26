package com.groupt1.hms.repository;

import com.groupt1.hms.entity.MaintenanceReport;
import com.groupt1.hms.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceReportRepository extends JpaRepository<MaintenanceReport, Integer> {
    List<MaintenanceReport> findByReportedBy_Username(String username);
    List<MaintenanceReport> findByReportedBy_Role_RoleName(String roleName);
    List<MaintenanceReport> findByReportedBy_Role_RoleNameNot(String roleName);
    void deleteByRoom_Id(RoomId roomId);
}
