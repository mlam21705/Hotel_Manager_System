package com.groupt1.hms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "SystemSetting")
public class SystemSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SettingID")
    private Integer settingId;

    @Column(name = "SettingCode", unique = true, nullable = false, length = 100)
    private String settingCode;

    @Column(name = "SettingValue", nullable = false, length = 255)
    private String settingValue;

    @Column(name = "Description", length = 500)
    private String description;

    public Integer getSettingId() {
        return settingId;
    }

    public void setSettingId(Integer settingId) {
        this.settingId = settingId;
    }

    public String getSettingCode() {
        return settingCode;
    }

    public void setSettingCode(String settingCode) {
        this.settingCode = settingCode;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
