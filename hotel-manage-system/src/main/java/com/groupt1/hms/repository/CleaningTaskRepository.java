package com.groupt1.hms.repository;

import com.groupt1.hms.entity.CleaningTask;
import com.groupt1.hms.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Integer> {
    List<CleaningTask> findByAssignedTo_Username(String username);
    List<CleaningTask> findByAssignedTo_UsernameAndTaskDate(String username, LocalDate date);
    List<CleaningTask> findByAssignedTo_UsernameAndTaskDateAndStatus(String username, LocalDate date, String status);
    void deleteByRoom_Id(RoomId roomId);
}
