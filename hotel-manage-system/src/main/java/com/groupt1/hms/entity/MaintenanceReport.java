package com.groupt1.hms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import com.groupt1.hms.entity.enums.ReportStatus;

@Entity
@Table(name = "MaintenanceReport")
public class MaintenanceReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportID")
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "BranchID", referencedColumnName = "BranchID"),
        @JoinColumn(name = "RoomNo", referencedColumnName = "RoomNo")
    })
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReportedBy", nullable = false)
    private UserAccount reportedBy;

    @Column(name = "IssueDescription", nullable = false, length = 500)
    private String issueDescription;

    @CreationTimestamp
    @Column(name = "ReportDate", updatable = false)
    private LocalDateTime reportDate;

    @Column(name = "ResolveStatus", length = 50)
    private ReportStatus resolveStatus = ReportStatus.PENDING;

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public UserAccount getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(UserAccount reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    public ReportStatus getResolveStatus() {
        return resolveStatus;
    }

    public void setResolveStatus(ReportStatus resolveStatus) {
        this.resolveStatus = resolveStatus;
    }
}
