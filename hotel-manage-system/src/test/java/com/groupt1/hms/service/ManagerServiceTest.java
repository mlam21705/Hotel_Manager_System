package com.groupt1.hms.service;

import com.groupt1.hms.entity.*;
import com.groupt1.hms.repository.*;
import com.groupt1.hms.service.impl.ManagerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManagerServiceTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomImageRepository roomImageRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UseServiceRepository useServiceRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CleaningTaskRepository cleaningTaskRepository;

    @Mock
    private MaintenanceReportRepository maintenanceReportRepository;

    @Mock
    private ReserveDetailRepository reserveDetailRepository;

    @InjectMocks
    private ManagerServiceImpl managerService;

    private RoomType roomType;

    @BeforeEach
    public void setUp() {
        roomType = new RoomType();
        roomType.setRoomTypeId(1);
        roomType.setTypeName("Standard");
        roomType.setBasePrice(new BigDecimal("100.00"));
    }

    @Test
    public void testGetAllRoomTypes() {
        when(roomTypeRepository.findAll()).thenReturn(Arrays.asList(roomType));
        List<RoomType> result = managerService.getAllRoomTypes();
        assertEquals(1, result.size());
        assertEquals("Standard", result.get(0).getTypeName());
    }

    @Test
    public void testGetBranchOccupancyReport() {
        // Setup mock rooms
        Room r1 = new Room();
        r1.setId(new RoomId(1, "101"));
        r1.setRoomStatus("Trống");

        Room r2 = new Room();
        r2.setId(new RoomId(1, "102"));
        r2.setRoomStatus("Đang sử dụng");

        Room r3 = new Room();
        r3.setId(new RoomId(1, "103"));
        r3.setRoomStatus("Đang dọn");

        when(roomRepository.findById_BranchId(1)).thenReturn(Arrays.asList(r1, r2, r3));

        Map<String, Object> report = managerService.getBranchOccupancyReport(1);

        assertEquals(1, report.get("branchId"));
        assertEquals(3L, report.get("totalRooms"));
        assertEquals(1L, report.get("occupiedRooms"));
        assertEquals(1L, report.get("cleaningRooms"));
        assertEquals(1L, report.get("emptyRooms"));
        assertEquals(33.333333333333336, (Double) report.get("occupancyRate"), 0.001);
    }

    @Test
    public void testGetRevenueReport() {
        when(invoiceRepository.sumRevenueByBranchAndMonth(1, 2026, 6))
                .thenReturn(new BigDecimal("3050000.00"));

        Map<String, Object> report = managerService.getRevenueReport(1, 2026, 6);

        assertEquals(1, report.get("branchId"));
        assertEquals(2026, report.get("year"));
        assertEquals(6, report.get("month"));
        assertEquals(new BigDecimal("3050000.00"), report.get("totalRevenue"));
    }

    @Test
    public void testGetRevenueReport_NullRevenue() {
        when(invoiceRepository.sumRevenueByBranchAndMonth(1, 2026, 7))
                .thenReturn(null);

        Map<String, Object> report = managerService.getRevenueReport(1, 2026, 7);

        assertEquals(BigDecimal.ZERO, report.get("totalRevenue"));
    }

    @Test
    public void testSaveRoomImage() {
        RoomImage image = new RoomImage();
        image.setImageUrl("test_url.jpg");
        when(roomImageRepository.save(image)).thenReturn(image);

        RoomImage saved = managerService.saveRoomImage(image);
        assertNotNull(saved);
        assertEquals("test_url.jpg", saved.getImageUrl());
        verify(roomImageRepository, times(1)).save(image);
    }

    @Test
    public void testDeleteRoomImage() {
        doNothing().when(roomImageRepository).deleteById(1);
        managerService.deleteRoomImage(1);
        verify(roomImageRepository, times(1)).deleteById(1);
    }

    @Test
    public void testGetImagesByRoomType() {
        RoomImage img = new RoomImage();
        img.setImageUrl("img.jpg");
        when(roomImageRepository.findByRoomType_RoomTypeId(1)).thenReturn(Arrays.asList(img));

        List<RoomImage> images = managerService.getImagesByRoomType(1);
        assertEquals(1, images.size());
        assertEquals("img.jpg", images.get(0).getImageUrl());
    }

    @Test
    public void testGetImagesByService() {
        RoomImage img = new RoomImage();
        img.setImageUrl("service_img.jpg");
        when(roomImageRepository.findByService_ServiceId(1)).thenReturn(Arrays.asList(img));

        List<RoomImage> images = managerService.getImagesByService(1);
        assertEquals(1, images.size());
        assertEquals("service_img.jpg", images.get(0).getImageUrl());
    }

    @Test
    public void testDeleteRoomType_NoRooms() {
        doNothing().when(roomImageRepository).deleteByRoomType_RoomTypeId(1);
        when(roomRepository.findByRoomType_RoomTypeId(1)).thenReturn(Arrays.asList());
        doNothing().when(roomTypeRepository).deleteById(1);

        managerService.deleteRoomType(1);

        verify(roomImageRepository, times(1)).deleteByRoomType_RoomTypeId(1);
        verify(roomRepository, times(1)).findByRoomType_RoomTypeId(1);
        verify(roomTypeRepository, times(1)).deleteById(1);
    }

    @Test
    public void testDeleteRoomType_WithRooms() {
        Room r1 = new Room();
        r1.setId(new RoomId(1, "101"));
        Room r2 = new Room();
        r2.setId(new RoomId(1, "102"));

        doNothing().when(roomImageRepository).deleteByRoomType_RoomTypeId(1);
        doNothing().when(roomImageRepository).deleteByRoom_Id(any(RoomId.class));
        when(roomRepository.findByRoomType_RoomTypeId(1)).thenReturn(Arrays.asList(r1, r2));
        doNothing().when(cleaningTaskRepository).deleteByRoom_Id(any(RoomId.class));
        doNothing().when(maintenanceReportRepository).deleteByRoom_Id(any(RoomId.class));
        doNothing().when(reserveDetailRepository).deleteByRoom_Id(any(RoomId.class));
        doNothing().when(roomRepository).deleteById(any(RoomId.class));
        doNothing().when(roomTypeRepository).deleteById(1);

        managerService.deleteRoomType(1);

        verify(roomImageRepository, times(1)).deleteByRoomType_RoomTypeId(1);
        verify(roomImageRepository, times(2)).deleteByRoom_Id(any(RoomId.class));
        verify(roomRepository, times(1)).findByRoomType_RoomTypeId(1);
        verify(cleaningTaskRepository, times(2)).deleteByRoom_Id(any(RoomId.class));
        verify(maintenanceReportRepository, times(2)).deleteByRoom_Id(any(RoomId.class));
        verify(reserveDetailRepository, times(2)).deleteByRoom_Id(any(RoomId.class));
        verify(roomRepository, times(2)).deleteById(any(RoomId.class));
        verify(roomTypeRepository, times(1)).deleteById(1);
    }

    @Test
    public void testDeleteRoom() {
        RoomId roomId = new RoomId(1, "101");
        doNothing().when(roomImageRepository).deleteByRoom_Id(roomId);
        doNothing().when(cleaningTaskRepository).deleteByRoom_Id(roomId);
        doNothing().when(maintenanceReportRepository).deleteByRoom_Id(roomId);
        doNothing().when(reserveDetailRepository).deleteByRoom_Id(roomId);
        doNothing().when(roomRepository).deleteById(roomId);

        managerService.deleteRoom(roomId);

        verify(roomImageRepository, times(1)).deleteByRoom_Id(roomId);
        verify(cleaningTaskRepository, times(1)).deleteByRoom_Id(roomId);
        verify(maintenanceReportRepository, times(1)).deleteByRoom_Id(roomId);
        verify(reserveDetailRepository, times(1)).deleteByRoom_Id(roomId);
        verify(roomRepository, times(1)).deleteById(roomId);
    }
}
