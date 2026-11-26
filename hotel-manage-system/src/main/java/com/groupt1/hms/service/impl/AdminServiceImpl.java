package com.groupt1.hms.service.impl;

import com.groupt1.hms.entity.MaintenanceReport;
import com.groupt1.hms.repository.MaintenanceReportRepository;
import com.groupt1.hms.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final MaintenanceReportRepository maintenanceReportRepository;

    @Override
    public List<MaintenanceReport> getCustomerReports() {
        return maintenanceReportRepository.findByReportedBy_Role_RoleName("Customer");
    }

    @Override
    public List<MaintenanceReport> getStaffReports() {
        return maintenanceReportRepository.findByReportedBy_Role_RoleNameNot("Customer");
    }

    @Override
    public MaintenanceReport getReportById(Integer id) {
        return maintenanceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + id));
    }

    @Override
    public void updateReportStatus(Integer id, com.groupt1.hms.entity.enums.ReportStatus status) {
        MaintenanceReport report = getReportById(id);
        report.setResolveStatus(status);
        maintenanceReportRepository.save(report);
    }
}
