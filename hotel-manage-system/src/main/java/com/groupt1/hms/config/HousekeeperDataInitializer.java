package com.groupt1.hms.config;

import com.groupt1.hms.entity.CleaningTask;
import com.groupt1.hms.entity.MaintenanceReport;
import com.groupt1.hms.entity.Room;
import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.entity.enums.ReportStatus;
import com.groupt1.hms.repository.CleaningTaskRepository;
import com.groupt1.hms.repository.MaintenanceReportRepository;
import com.groupt1.hms.repository.RoomRepository;
import com.groupt1.hms.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class HousekeeperDataInitializer implements CommandLineRunner {

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private MaintenanceReportRepository maintenanceReportRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Override
    public void run(String... args) throws Exception {
        // Find housekeeper user
        UserAccount housekeeper = userAccountRepository.findByUsername("housek01").orElse(null);
        if (housekeeper == null) {
            List<UserAccount> allUsers = userAccountRepository.findAll();
            for (UserAccount u : allUsers) {
                if (u.getRole() != null && "Housekeeper".equalsIgnoreCase(u.getRole().getRoleName())) {
                    housekeeper = u;
                    break;
                }
            }
        }

        if (housekeeper == null) {
            System.out.println("[HousekeeperDataInitializer] No housekeeper user found, skipping data initialization.");
            return;
        }

        // Initialize Cleaning Tasks if none exist
        if (cleaningTaskRepository.count() == 0) {
            System.out.println("[HousekeeperDataInitializer] No cleaning tasks found. Initializing seed data...");
            List<Room> rooms = roomRepository.findAll();
            if (!rooms.isEmpty()) {
                // Task 1: Pending for Room 1
                CleaningTask task1 = new CleaningTask();
                task1.setRoom(rooms.get(0));
                task1.setAssignedTo(housekeeper);
                task1.setTaskDate(LocalDate.now());
                task1.setStatus("Pending");
                task1.setNotes("Regular morning cleanup");
                cleaningTaskRepository.save(task1);

                if (rooms.size() > 1) {
                    // Task 2: Cleaning for Room 2
                    CleaningTask task2 = new CleaningTask();
                    task2.setRoom(rooms.get(1));
                    task2.setAssignedTo(housekeeper);
                    task2.setTaskDate(LocalDate.now());
                    task2.setStatus("Cleaning");
                    task2.setNotes("Sanitize high touch surfaces");
                    cleaningTaskRepository.save(task2);
                }

                if (rooms.size() > 2) {
                    // Task 3: Completed for Room 3
                    CleaningTask task3 = new CleaningTask();
                    task3.setRoom(rooms.get(2));
                    task3.setAssignedTo(housekeeper);
                    task3.setTaskDate(LocalDate.now());
                    task3.setStatus("Completed");
                    task3.setNotes("Check-out cleanup complete");
                    cleaningTaskRepository.save(task3);
                }
                System.out.println("[HousekeeperDataInitializer] Seeded cleaning tasks successfully.");
            } else {
                System.out.println("[HousekeeperDataInitializer] No rooms found in the database. Cannot seed tasks.");
            }
        }

        // Ensure any room that is marked as "Đang dọn" in the database has an active cleaning task
        List<Room> allRooms = roomRepository.findAll();
        for (Room r : allRooms) {
            if ("Đang dọn".equalsIgnoreCase(r.getRoomStatus())) {
                boolean hasActiveTask = cleaningTaskRepository.findAll().stream()
                        .anyMatch(t -> t.getRoom() != null 
                                && t.getRoom().getId().equals(r.getId())
                                && !"Completed".equalsIgnoreCase(t.getStatus()));
                if (!hasActiveTask) {
                    CleaningTask task = new CleaningTask();
                    task.setRoom(r);
                    task.setAssignedTo(housekeeper);
                    task.setTaskDate(LocalDate.now());
                    task.setStatus("Pending");
                    task.setNotes("Active cleanup required (auto-detected room status: Đang dọn)");
                    cleaningTaskRepository.save(task);
                    System.out.println("[HousekeeperDataInitializer] Auto-created cleaning task for room status 'Đang dọn': Branch " 
                            + r.getId().getBranchId() + ", Room " + r.getId().getRoomNo());
                }
            }
        }

        // Initialize Maintenance Reports if none exist
        if (maintenanceReportRepository.count() == 0) {
            System.out.println("[HousekeeperDataInitializer] No maintenance reports found. Initializing seed data...");
            List<CleaningTask> tasks = cleaningTaskRepository.findByAssignedTo_Username(housekeeper.getUsername());
            if (!tasks.isEmpty()) {
                Room room = tasks.get(0).getRoom();
                if (room != null) {
                    MaintenanceReport report = new MaintenanceReport();
                    report.setRoom(room);
                    report.setReportedBy(housekeeper);
                    report.setIssueDescription("Leaking pipe in bathroom");
                    report.setResolveStatus(ReportStatus.PENDING);
                    maintenanceReportRepository.save(report);
                    System.out.println("[HousekeeperDataInitializer] Seeded maintenance report successfully.");
                }
            } else {
                System.out.println("[HousekeeperDataInitializer] No housekeeper assigned tasks found to link problems.");
            }
        }
    }
}
