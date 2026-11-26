package com.groupt1.hms.service;

import com.groupt1.hms.dto.WalkInBookingRequest;
import com.groupt1.hms.entity.*;
import com.groupt1.hms.repository.*;
import com.groupt1.hms.service.impl.ReceptionistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceptionistServiceTest {

    @Mock
    private ReserveRepository reserveRepository;

    @Mock
    private ReserveDetailRepository reserveDetailRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UseServiceRepository useServiceRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private CleaningTaskRepository cleaningTaskRepository;

    @InjectMocks
    private ReceptionistServiceImpl receptionistService;

    private Reserve reserve;
    private ReserveDetail reserveDetail;
    private Guest guest;
    private Room room;
    private RoomId roomId;
    private RoomType roomType;

    @BeforeEach
    public void setUp() {
        guest = new Guest();
        guest.setGuestId(1);
        guest.setGuestName("Test Guest");
        guest.setCccd("123456789012");
        guest.setPhoneNumber("0987654321");

        roomId = new RoomId(1, "101");
        roomType = new RoomType();
        roomType.setRoomTypeId(1);
        roomType.setTypeName("Deluxe");
        roomType.setCapacity(2);
        roomType.setBasePrice(new BigDecimal("1000000"));

        room = new Room();
        room.setId(roomId);
        room.setRoomType(roomType);
        room.setRoomStatus("Trống");

        reserve = new Reserve();
        reserve.setReservationId(10);
        reserve.setGuest(guest);
        reserve.setReservationStatus("Pending");
        reserve.setTotalAmount(new BigDecimal("1000000"));

        reserveDetail = new ReserveDetail();
        reserveDetail.setReserveDetailId(100);
        reserveDetail.setReserve(reserve);
        reserveDetail.setRoom(room);
        reserveDetail.setCheckInDate(LocalDate.now());
        reserveDetail.setCheckOutDate(LocalDate.now().plusDays(2));
        reserveDetail.setNumberOfGuest(1);
        reserveDetail.setPrice(new BigDecimal("1000000"));

        reserve.setReserveDetails(new ArrayList<>(Arrays.asList(reserveDetail)));
    }

    @Test
    public void testConfirmReservationSuccess() {
        when(reserveRepository.findById(10)).thenReturn(Optional.of(reserve));
        when(reserveRepository.save(any(Reserve.class))).thenAnswer(i -> i.getArgument(0));

        Reserve confirmed = receptionistService.confirmReservation(10);
        assertEquals("Confirmed", confirmed.getReservationStatus());
        verify(reserveRepository, times(1)).save(reserve);
    }

    @Test
    public void testConfirmReservationNotPending() {
        reserve.setReservationStatus("Confirmed");
        when(reserveRepository.findById(10)).thenReturn(Optional.of(reserve));

        assertThrows(IllegalStateException.class, () -> receptionistService.confirmReservation(10));
    }

    @Test
    public void testCheckInSuccess() {
        reserve.setReservationStatus("Confirmed");
        when(reserveRepository.findById(10)).thenReturn(Optional.of(reserve));
        when(reserveDetailRepository.findById(100)).thenReturn(Optional.of(reserveDetail));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(reserveRepository.save(any(Reserve.class))).thenAnswer(i -> i.getArgument(0));

        Reserve result = receptionistService.checkIn(10, 100);
        assertEquals("Checked-In", result.getReservationStatus());
        assertEquals("Đang sử dụng", room.getRoomStatus());
    }

    @Test
    public void testCheckInRoomNotVacant() {
        reserve.setReservationStatus("Confirmed");
        room.setRoomStatus("Đang sử dụng");
        when(reserveRepository.findById(10)).thenReturn(Optional.of(reserve));
        when(reserveDetailRepository.findById(100)).thenReturn(Optional.of(reserveDetail));

        assertThrows(IllegalStateException.class, () -> receptionistService.checkIn(10, 100));
    }

    @Test
    public void testCheckOutSuccess() {
        reserve.setReservationStatus("Checked-In");
        when(reserveDetailRepository.findById(100)).thenReturn(Optional.of(reserveDetail));
        when(invoiceRepository.findByReserve_ReservationId(10)).thenReturn(Optional.empty());
        when(reserveDetailRepository.findByReserve_ReservationId(10)).thenReturn(Arrays.asList(reserveDetail));
        
        Service s = new Service();
        s.setServiceId(1);
        s.setServiceName("Water");
        s.setServicePrice(new BigDecimal("50000"));

        UseService us = new UseService();
        us.setService(s);
        us.setQuantity(2);
        us.setPriceAtTime(new BigDecimal("50000"));

        UserAccount housekeeper = new UserAccount();
        housekeeper.setUserId(4);
        housekeeper.setUsername("housek01");
        when(userAccountRepository.findByUsername("housek01")).thenReturn(Optional.of(housekeeper));
        when(cleaningTaskRepository.findByAssignedTo_Username("housek01")).thenReturn(new ArrayList<>());
        when(cleaningTaskRepository.save(any(CleaningTask.class))).thenAnswer(i -> i.getArgument(0));

        when(useServiceRepository.findByReserveDetail_ReserveDetailId(100)).thenReturn(Arrays.asList(us));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(reserveRepository.save(any(Reserve.class))).thenAnswer(i -> i.getArgument(0));

        Invoice inv = receptionistService.checkOut(100);
        
        // 2 nights * 1,000,000 + 2 * 50,000 = 2,100,000
        assertEquals(new BigDecimal("2100000"), inv.getTotalAmount());
        assertEquals("Đã thanh toán", inv.getInvoiceStatus());
        assertEquals("Checked-Out", reserve.getReservationStatus());
        assertEquals("Đang dọn", room.getRoomStatus());
    }

    @Test
    public void testCreateWalkInReservationSuccess() {
        WalkInBookingRequest req = WalkInBookingRequest.builder()
                .guestName("WalkIn Guest")
                .cccd("123123123123")
                .phone("0111222333")
                .address("HCMC")
                .branchId(1)
                .roomNo("101")
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .numberOfGuest(1)
                .build();

        when(guestRepository.findByCccd("123123123123")).thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenAnswer(i -> i.getArgument(0));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reserveRepository.save(any(Reserve.class))).thenAnswer(i -> i.getArgument(0));
        when(reserveDetailRepository.save(any(ReserveDetail.class))).thenAnswer(i -> i.getArgument(0));

        Reserve res = receptionistService.createWalkInReservation(req);
        assertNotNull(res);
        assertEquals("Checked-In", res.getReservationStatus());
        assertEquals("Đang sử dụng", room.getRoomStatus());
    }
}
