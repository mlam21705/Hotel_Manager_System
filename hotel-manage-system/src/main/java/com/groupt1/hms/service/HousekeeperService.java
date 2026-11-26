package com.groupt1.hms.service;

import com.groupt1.hms.entity.CleaningTask;
import com.groupt1.hms.entity.MaintenanceReport;
import com.groupt1.hms.entity.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HousekeeperService {
    List<CleaningTask> getAssignedTasks(String username, LocalDate date, String status);
    Optional<CleaningTask> getTaskById(Integer taskId);
    void updateCleaningStatus(Integer taskId, String taskStatus, String roomStatus, String username);
    void reportRoomProblem(String username, Integer branchId, String roomNo, String issueDescription);
    List<MaintenanceReport> getReportedProblems(String username);
    List<Room> getAllRooms();
}
