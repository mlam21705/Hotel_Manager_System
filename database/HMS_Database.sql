-- =========================================================================
-- KHỞI TẠO DATABASE (MySQL)
-- =========================================================================
CREATE DATABASE IF NOT EXISTS FullHotelManagementSystem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE FullHotelManagementSystem;

-- =========================================================================
-- A. MODULE ADMIN & USER (Phân quyền và tài khoản)
-- =========================================================================

-- 1. Bảng Role (Phân quyền)
CREATE TABLE Role (
    RoleID INT AUTO_INCREMENT PRIMARY KEY,
    RoleName VARCHAR(50) NOT NULL,
    Description VARCHAR(255)
);

-- 2. Bảng Permission (Quyền hạn - đã bỏ)


-- 3. Bảng RolePermission (Trung gian giữa Role và Permission -đã bỏ-)

-- 4. Bảng UserAccount (Tài khoản hệ thống)
CREATE TABLE UserAccount (
    UserID INT AUTO_INCREMENT PRIMARY KEY,
    Username VARCHAR(50) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Email VARCHAR(100) UNIQUE,
    RoleID INT,
    ResetCode VARCHAR(6),
    ResetCodeExpiry DATETIME,
    Status VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (RoleID) REFERENCES Role(RoleID)
);

-- =========================================================================
-- B. MODULE MANAGER (Quản lý Khách sạn & Phòng)
-- =========================================================================

-- 6. Bảng RoomType (Loại phòng)
CREATE TABLE RoomType (
    RoomTypeID INT AUTO_INCREMENT PRIMARY KEY,
    TypeName VARCHAR(50) NOT NULL,
    Capacity INT NOT NULL,
    BasePrice DECIMAL(12,2) NOT NULL,
    Description VARCHAR(500)
);

-- 7. Bảng Room (Phòng - dùng khóa chính kép BranchID + RoomNo)
CREATE TABLE Room (
    BranchID INT NOT NULL,
    RoomNo VARCHAR(10) NOT NULL,
    RoomTypeID INT NOT NULL,
    Floor INT,
    RoomStatus VARCHAR(50) DEFAULT 'Trống',
    PRIMARY KEY (BranchID, RoomNo),
    FOREIGN KEY (RoomTypeID) REFERENCES RoomType(RoomTypeID)
);

-- 8. Bảng RoomImage (Hình ảnh phòng)
CREATE TABLE RoomImage (
    ImageID INT AUTO_INCREMENT PRIMARY KEY,
    BranchID INT NOT NULL,
    RoomNo VARCHAR(10) NOT NULL,
    ImageURL VARCHAR(255) NOT NULL,
    IsPrimary BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (BranchID, RoomNo) REFERENCES Room(BranchID, RoomNo) ON DELETE CASCADE
);

-- =========================================================================
-- C. MODULE CUSTOMER & RECEPTIONIST (Khách hàng & Đặt phòng)
-- =========================================================================

-- 9. Bảng Guest (Hồ sơ khách hàng)
CREATE TABLE Guest (
    GuestID INT AUTO_INCREMENT PRIMARY KEY,
    UserID INT NULL, -- Link tài khoản (nếu khách hàng đăng ký trên hệ thống)
    Guest_Name VARCHAR(100) NOT NULL,
    CCCD VARCHAR(20) UNIQUE NOT NULL,
    PhoneNumber VARCHAR(20),
    Address VARCHAR(255),
    FOREIGN KEY (UserID) REFERENCES UserAccount(UserID)
);

-- 10. Bảng Reserve (Header của đơn đặt phòng)
CREATE TABLE Reserve (
    ReservationID INT AUTO_INCREMENT PRIMARY KEY,
    GuestID INT NOT NULL,
    ReserveDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    ReservationStatus VARCHAR(50) DEFAULT 'Pending',
    TotalAmount DECIMAL(12,2) DEFAULT 0,
    FOREIGN KEY (GuestID) REFERENCES Guest(GuestID)
);

-- 11. Bảng ReserveDetail (Chi tiết các phòng trong đơn đặt phòng)
CREATE TABLE ReserveDetail (
    ReserveDetailID INT AUTO_INCREMENT PRIMARY KEY,
    ReservationID INT NOT NULL,
    BranchID INT NOT NULL,
    RoomNo VARCHAR(10) NOT NULL,
    CheckInDate DATE NOT NULL,
    CheckOutDate DATE NOT NULL,
    NumberOfGuest INT NOT NULL,
    Price DECIMAL(12,2) NOT NULL, -- Giá phòng tại thời điểm chốt
    FOREIGN KEY (ReservationID) REFERENCES Reserve(ReservationID) ON DELETE CASCADE,
    FOREIGN KEY (BranchID, RoomNo) REFERENCES Room(BranchID, RoomNo)
);

-- 12. Bảng Service (Dịch vụ)
CREATE TABLE Service (
    ServiceID INT AUTO_INCREMENT PRIMARY KEY,
    ServiceName VARCHAR(100) NOT NULL,
    ServicePrice DECIMAL(12,2) NOT NULL
);

-- 13. Bảng UseService (Dịch vụ được dùng ở phòng nào/ ReserveDetailID nào)
CREATE TABLE UseService (
    UseID INT AUTO_INCREMENT PRIMARY KEY,
    ReserveDetailID INT NOT NULL,
    ServiceID INT NOT NULL,
    UseDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    Quantity INT DEFAULT 1,
    PriceAtTime DECIMAL(12,2) NOT NULL, -- Giá dịch vụ lúc khách gọi
    FOREIGN KEY (ReserveDetailID) REFERENCES ReserveDetail(ReserveDetailID) ON DELETE CASCADE,
    FOREIGN KEY (ServiceID) REFERENCES Service(ServiceID)
);

-- 14. Bảng Invoice (Hóa đơn)
CREATE TABLE Invoice (
    InvoiceID INT AUTO_INCREMENT PRIMARY KEY,
    ReservationID INT UNIQUE NOT NULL,
    InvoiceDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    InvoiceStatus VARCHAR(50) DEFAULT 'Chưa thanh toán',
    TotalAmount DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (ReservationID) REFERENCES Reserve(ReservationID)
);

-- =========================================================================
-- D. MODULE HOUSEKEEPER (Dọn dẹp & Bảo trì)
-- =========================================================================

-- 15. Bảng CleaningTask (Giao việc dọn phòng)
CREATE TABLE CleaningTask (
    TaskID INT AUTO_INCREMENT PRIMARY KEY,
    BranchID INT NOT NULL,
    RoomNo VARCHAR(10) NOT NULL,
    AssignedTo INT, -- Mã Housekeeper (UserAccount)
    TaskDate DATE DEFAULT (CURRENT_DATE),
    Status VARCHAR(50) DEFAULT 'Pending',
    Notes VARCHAR(500),
    FOREIGN KEY (BranchID, RoomNo) REFERENCES Room(BranchID, RoomNo),
    FOREIGN KEY (AssignedTo) REFERENCES UserAccount(UserID)
);

-- 16. Bảng MaintenanceReport (Báo cáo sự cố bảo trì phòng)
CREATE TABLE MaintenanceReport (
    ReportID INT AUTO_INCREMENT PRIMARY KEY,
    BranchID INT NOT NULL,
    RoomNo VARCHAR(10) NOT NULL,
    ReportedBy INT NOT NULL, -- Người báo cáo
    IssueDescription VARCHAR(500) NOT NULL,
    ReportDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    ResolveStatus VARCHAR(50) DEFAULT 'Open',
    FOREIGN KEY (BranchID, RoomNo) REFERENCES Room(BranchID, RoomNo),
    FOREIGN KEY (ReportedBy) REFERENCES UserAccount(UserID)
);

-- =========================================================================
-- E. CONFIGURATION & LOGS (Admin Hệ thống)
-- =========================================================================

-- 17. Bảng SystemSetting (Cấu hình khách sạn - Hủy phòng, Giá cọc...)
CREATE TABLE SystemSetting (
    SettingID INT AUTO_INCREMENT PRIMARY KEY,
    SettingCode VARCHAR(100) UNIQUE NOT NULL,
    SettingValue VARCHAR(255) NOT NULL,
    Description VARCHAR(500)
);

-- =========================================================================
-- F. DUMMY DATA INSERTION (Dữ liệu mẫu)
-- =========================================================================

-- 1. Insert Role
INSERT INTO Role (RoleName, Description) VALUES
('Admin', 'System administrator'),
('Manager', 'Hotel manager'),
('Receptionist', 'Front desk staff'),
('Housekeeper', 'Room cleaning staff'),
('Customer', 'Registered guest account');


-- 4. Insert UserAccount
INSERT INTO UserAccount (Username, PasswordHash, Email, RoleID, Status) VALUES
('admin', '1111111', 'admin@hms.com', 1, 'ACTIVE'),
('manager01', '1111111', 'manager01@hms.com', 2, 'ACTIVE'),
('recept01', '1111111', 'reception01@hms.com', 3, 'ACTIVE'),
('housek01', '1111111', 'housekeeper01@hms.com', 4, 'ACTIVE'),
('johndoe', '1111111', 'johndoe@gmail.com', 5, 'ACTIVE');

-- 6. Insert RoomType
INSERT INTO RoomType (TypeName, Capacity, BasePrice, Description) VALUES
('Standard Single', 1, 450000, 'Single bed room'),
('Standard Double', 2, 650000, 'Double bed room'),
('Superior Twin', 2, 800000, 'Two single beds'),
('Superior Double', 2, 900000, 'Superior double room'),
('Deluxe Twin', 2, 1200000, 'Deluxe twin room'),
('Deluxe Double', 2, 1350000, 'Deluxe double room'),
('Family Room', 4, 1800000, 'Family room'),
('Executive Room', 2, 2200000, 'Executive business room'),
('Junior Suite', 3, 2800000, 'Junior suite'),
('Presidential Suite', 6, 6000000, 'Luxury presidential suite');

-- 7. Insert Room
INSERT INTO Room (BranchID, RoomNo, RoomTypeID, Floor, RoomStatus) VALUES
(1,'101',1,1,'Trống'),
(1,'102',2,1,'Trống'),
(1,'201',3,2,'Trống'),
(1,'202',4,2,'Trống'),
(1,'301',10,3,'Trống');

INSERT INTO Room (BranchID, RoomNo, RoomTypeID, Floor, RoomStatus) VALUES
(2,'101',1,1,'Trống'),
(2,'102',5,1,'Trống'),
(2,'201',6,2,'Trống'),
(2,'202',7,2,'Trống'),
(2,'301',9,3,'Trống');

INSERT INTO Room (BranchID, RoomNo, RoomTypeID, Floor, RoomStatus) VALUES
(3,'101',2,1,'Trống'),
(3,'102',4,1,'Trống'),
(3,'201',5,2,'Trống'),
(3,'202',8,2,'Trống'),
(3,'301',10,3,'Trống');

-- 8. Insert RoomImage


-- 9. Insert Guest
INSERT INTO Guest (UserID, Guest_Name, CCCD, PhoneNumber, Address) VALUES
(5, 'John Doe', '001090123456', '0901234567', '123 ABC Street, Hanoi'),
(NULL, 'Jane Smith', '001090654321', '0912345678', '456 XYZ Street, HCM City');

-- 10. Insert Reserve
INSERT INTO Reserve (GuestID, ReserveDate, ReservationStatus, TotalAmount) VALUES
(1, '2026-06-01 10:00:00', 'Checked-Out', 3600000.00),
(2, '2026-06-15 14:30:00', 'Pending', 500000.00);

-- 11. Insert ReserveDetail
INSERT INTO ReserveDetail (ReservationID, BranchID, RoomNo, CheckInDate, CheckOutDate, NumberOfGuest, Price) VALUES
(1, 1, '202', '2026-06-10', '2026-06-12', 3, 2500000.00),
(2, 1, '101', '2026-06-20', '2026-06-21', 1, 500000.00);

-- 12. Insert Service
INSERT INTO Service (ServiceName, ServicePrice) VALUES
('Breakfast Buffet', 200000),
('Laundry Service', 80000),
('Airport Pickup', 450000),
('Spa Package', 700000),
('Massage', 500000),
('Mini Bar', 150000),
('Room Decoration', 350000),
('Extra Bed', 300000),
('Swimming Pool Ticket', 100000),
('Gym Access', 120000);

-- 13. Insert UseService
INSERT INTO UseService (ReserveDetailID, ServiceID, UseDate, Quantity, PriceAtTime) VALUES
(1, 1, '2026-06-11 07:30:00', 3, 150000.00),
(1, 2, '2026-06-11 18:00:00', 2, 50000.00);

-- 14. Insert Invoice
INSERT INTO Invoice (ReservationID, InvoiceDate, InvoiceStatus, TotalAmount) VALUES
(1, '2026-06-12 10:00:00', 'Đã thanh toán', 3050000.00);

-- 15. Insert CleaningTask
INSERT INTO CleaningTask (BranchID, RoomNo, AssignedTo, TaskDate, Status, Notes) VALUES
(1, '201', 4, '2026-06-17', 'Pending', 'Regular cleaning after check-out');

-- 16. Insert MaintenanceReport
INSERT INTO MaintenanceReport (BranchID, RoomNo, ReportedBy, IssueDescription, ReportDate, ResolveStatus) VALUES
(1, '102', 4, 'Air conditioner is not working', '2026-06-16 09:00:00', 'Open');

-- 17. Insert SystemSetting
INSERT INTO SystemSetting (SettingCode, SettingValue, Description) VALUES
('CHECKIN_TIME', '14:00', 'Standard check-in time'),
('CHECKOUT_TIME', '12:00', 'Standard check-out time'),
('CANCELLATION_FEE_PCT', '20', 'Percentage fee for cancellation within 24h');

