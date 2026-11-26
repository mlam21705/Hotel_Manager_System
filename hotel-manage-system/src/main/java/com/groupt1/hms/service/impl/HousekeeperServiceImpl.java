package com.groupt1.hms.service.impl;

import com.groupt1.hms.entity.CleaningTask;
import com.groupt1.hms.entity.MaintenanceReport;
import com.groupt1.hms.entity.Room;
import com.groupt1.hms.entity.RoomId;
import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.repository.RoomRepository;
import com.groupt1.hms.repository.UserAccountRepository;
import com.groupt1.hms.repository.CleaningTaskRepository;
import com.groupt1.hms.repository.MaintenanceReportRepository;
import com.groupt1.hms.entity.enums.ReportStatus;

import com.groupt1.hms.service.HousekeeperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HousekeeperServiceImpl implements HousekeeperService {

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private MaintenanceReportRepository maintenanceReportRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public List<CleaningTask> getAssignedTasks(String username, LocalDate date, String status) {
        boolean filterStatus = status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status);
        LocalDate taskDate = (date != null) ? date : LocalDate.now();

        // Dynamically ensure every room in the database has a task assigned to this housekeeper on taskDate
        UserAccount housekeeper = userAccountRepository.findByUsername(username).orElse(null);
        if (housekeeper != null) {
            List<Room> rooms = roomRepository.findAll();
            for (Room room : rooms) {
                boolean exists = cleaningTaskRepository.findAll().stream()
                        .anyMatch(t -> t.getRoom() != null 
                                && t.getRoom().getId().equals(room.getId()) 
                                && taskDate.equals(t.getTaskDate()));
                if (!exists) {
                    CleaningTask task = new CleaningTask();
                    task.setRoom(room);
                    task.setAssignedTo(housekeeper);
                    task.setTaskDate(taskDate);
                    
                    // Determine task status based on room status
                    if ("Đang dọn".equalsIgnoreCase(room.getRoomStatus())) {
                        task.setStatus("Pending");
                        task.setNotes("Room checked out, needs cleaning.");
                    } else if ("Trống".equalsIgnoreCase(room.getRoomStatus())) {
                        task.setStatus("Completed");
                        task.setNotes("Clean");
                    } else {
                        task.setStatus("Completed");
                        task.setNotes("Occupied");
                    }
                    cleaningTaskRepository.save(task);
                }
            }
        }

        // Return all tasks in the database (optionally filtered by date and status)
        List<CleaningTask> allTasks = cleaningTaskRepository.findAll();
        return allTasks.stream()
                .filter(t -> (date == null || date.equals(t.getTaskDate())))
                .filter(t -> (!filterStatus || status.trim().equalsIgnoreCase(t.getStatus())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CleaningTask> getTaskById(Integer taskId) {
        return cleaningTaskRepository.findById(taskId);
    }

    @Override
    @Transactional
    public void updateCleaningStatus(Integer taskId, String taskStatus, String roomStatus, String username) {
        CleaningTask task = cleaningTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaning task not found."));



        // Update task status
        if (taskStatus != null && !taskStatus.trim().isEmpty()) {
            task.setStatus(taskStatus.trim());
        }

        // Update room status
        if (roomStatus != null && !roomStatus.trim().isEmpty()) {
            Room room = task.getRoom();
            if (room != null) {
                room.setRoomStatus(roomStatus.trim());
                roomRepository.save(room);
            }
        }

        cleaningTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void reportRoomProblem(String username, Integer branchId, String roomNo, String issueDescription) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));

        RoomId roomId = new RoomId(branchId, roomNo.trim());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room " + roomNo + " in Branch " + branchId + " not found."));

        MaintenanceReport report = new MaintenanceReport();
        report.setRoom(room);
        report.setReportedBy(user);
        report.setIssueDescription(issueDescription);
        report.setResolveStatus(ReportStatus.PENDING);

        maintenanceReportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceReport> getReportedProblems(String username) {
        return maintenanceReportRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}
