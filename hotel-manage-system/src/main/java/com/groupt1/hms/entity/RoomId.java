package com.groupt1.hms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RoomId implements Serializable {
    
    @Column(name = "BranchID")
    private Integer branchId;
    
    @Column(name = "RoomNo", length = 10)
    private String roomNo;

    // Default constructor
    public RoomId() {}

    public RoomId(Integer branchId, String roomNo) {
        this.branchId = branchId;
        this.roomNo = roomNo;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomId)) return false;
        RoomId roomId = (RoomId) o;
        return Objects.equals(branchId, roomId.branchId) &&
               Objects.equals(roomNo, roomId.roomNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchId, roomNo);
    }
}
