package com.groupt1.hms.service;

import com.groupt1.hms.entity.MaintenanceReport;
import java.util.List;

import com.groupt1.hms.entity.enums.ReportStatus;

public interface AdminService {
    List<MaintenanceReport> getCustomerReports();
    List<MaintenanceReport> getStaffReports();
    MaintenanceReport getReportById(Integer id);
    void updateReportStatus(Integer id, ReportStatus status);
}
