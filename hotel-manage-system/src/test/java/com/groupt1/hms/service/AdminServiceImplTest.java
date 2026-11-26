package com.groupt1.hms.service;

import com.groupt1.hms.entity.MaintenanceReport;
import com.groupt1.hms.repository.MaintenanceReportRepository;
import com.groupt1.hms.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private MaintenanceReportRepository maintenanceReportRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetCustomerReports() {
        // Arrange
        MaintenanceReport report1 = new MaintenanceReport();
        report1.setReportId(1);
        List<MaintenanceReport> expectedReports = Arrays.asList(report1);

        when(maintenanceReportRepository.findByReportedBy_Role_RoleName("Customer")).thenReturn(expectedReports);

        // Act
        List<MaintenanceReport> actualReports = adminService.getCustomerReports();

        // Assert
        assertEquals(1, actualReports.size());
        assertEquals(1, actualReports.get(0).getReportId());
        verify(maintenanceReportRepository).findByReportedBy_Role_RoleName("Customer");
    }

    @Test
    void testGetStaffReports() {
        // Arrange
        MaintenanceReport report2 = new MaintenanceReport();
        report2.setReportId(2);
        List<MaintenanceReport> expectedReports = Arrays.asList(report2);

        when(maintenanceReportRepository.findByReportedBy_Role_RoleNameNot("Customer")).thenReturn(expectedReports);

        // Act
        List<MaintenanceReport> actualReports = adminService.getStaffReports();

        // Assert
        assertEquals(1, actualReports.size());
        assertEquals(2, actualReports.get(0).getReportId());
        verify(maintenanceReportRepository).findByReportedBy_Role_RoleNameNot("Customer");
    }
}
