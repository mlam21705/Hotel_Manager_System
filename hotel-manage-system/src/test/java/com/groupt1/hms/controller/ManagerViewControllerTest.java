package com.groupt1.hms.controller;

import com.groupt1.hms.entity.*;
import com.groupt1.hms.service.ManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ManagerViewControllerTest {

    @Mock
    private ManagerService managerService;

    @InjectMocks
    private ManagerViewController managerViewController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSaveRoomType_Success() {
        RoomType rt = new RoomType();
        rt.setRoomTypeId(1);
        rt.setTypeName("Deluxe Suite");
        rt.setCapacity(2);
        rt.setBasePrice(new BigDecimal("1200000"));
        rt.setDescription("A premium room");

        when(managerService.getRoomTypeById(1)).thenReturn(java.util.Optional.of(rt));
        when(managerService.saveRoomType(any(RoomType.class))).thenReturn(rt);
        when(managerService.getImagesByRoomType(1)).thenReturn(new ArrayList<>());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        MultipartFile[] files = new MockMultipartFile[] {
                new MockMultipartFile("imageFiles", "room1.jpg", "image/jpeg", "image_data_1".getBytes())
        };
        List<Integer> keepImageIds = new ArrayList<>();

        String result = managerViewController.saveRoomType(
                1, "Deluxe Suite", 2, new BigDecimal("1200000"), "A premium room", keepImageIds, files, redirectAttributes
        );

        assertEquals("redirect:/manager/room-types", result);
        assertEquals("Room Type saved successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
        verify(managerService, times(1)).saveRoomType(any(RoomType.class));
        verify(managerService, atLeastOnce()).saveRoomImage(any(RoomImage.class));
    }

    @Test
    public void testSaveRoomType_NegativePrice() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        MultipartFile[] files = new MultipartFile[0];
        List<Integer> keepImageIds = new ArrayList<>();

        String result = managerViewController.saveRoomType(
                null, "Deluxe Suite", 2, new BigDecimal("-50000"), "A premium room", keepImageIds, files, redirectAttributes
        );

        assertEquals("redirect:/manager/room-types", result);
        assertEquals("Base Price cannot be negative.", redirectAttributes.getFlashAttributes().get("errorMessage"));
        verify(managerService, never()).saveRoomType(any(RoomType.class));
    }

    @Test
    public void testSaveService_Success() {
        Service svc = new Service();
        svc.setServiceId(1);
        svc.setServiceName("Premium Spa");
        svc.setServicePrice(new BigDecimal("350000"));

        when(managerService.getServiceById(1)).thenReturn(java.util.Optional.of(svc));
        when(managerService.saveService(any(Service.class))).thenReturn(svc);
        when(managerService.getImagesByService(1)).thenReturn(new ArrayList<>());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        MultipartFile[] files = new MockMultipartFile[] {
                new MockMultipartFile("imageFiles", "spa1.jpg", "image/jpeg", "image_data_1".getBytes())
        };
        List<Integer> keepImageIds = new ArrayList<>();

        String result = managerViewController.saveService(
                1, "Premium Spa", new BigDecimal("350000"), keepImageIds, files, redirectAttributes
        );

        assertEquals("redirect:/manager/services", result);
        assertEquals("Service saved successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));
        verify(managerService, times(1)).saveService(any(Service.class));
        verify(managerService, atLeastOnce()).saveRoomImage(any(RoomImage.class));
    }
}
