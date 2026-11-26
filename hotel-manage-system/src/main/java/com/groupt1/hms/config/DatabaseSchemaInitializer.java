package com.groupt1.hms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
public class DatabaseSchemaInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 1. Find constraint names of the foreign key to Room and drop them
            String getConstraintSql = 
                "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'RoomImage' " +
                "AND REFERENCED_TABLE_NAME = 'Room'";
            
            try {
                java.util.List<String> constraints = jdbcTemplate.queryForList(getConstraintSql, String.class);
                for (String constraintName : constraints) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE RoomImage DROP FOREIGN KEY " + constraintName);
                    } catch (Exception e) {
                        System.out.println("Could not drop foreign key constraint: " + constraintName + " - " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not check/drop constraints via information_schema: " + e.getMessage());
            }

            // 2. Make BranchID and RoomNo nullable.
            try {
                jdbcTemplate.execute("ALTER TABLE RoomImage MODIFY COLUMN BranchID INT NULL");
                jdbcTemplate.execute("ALTER TABLE RoomImage MODIFY COLUMN RoomNo VARCHAR(10) NULL");
            } catch (Exception e) {
                System.out.println("Could not modify Room columns to NULL: " + e.getMessage());
            }

            // 3. Re-add Room foreign key constraint.
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE RoomImage ADD CONSTRAINT fk_roomimage_room " +
                    "FOREIGN KEY (BranchID, RoomNo) REFERENCES Room(BranchID, RoomNo) ON DELETE CASCADE"
                );
            } catch (Exception e) {
                // Ignore if constraint already exists
            }

            // 3.5 Drop and recreate UseService foreign key to Service with ON DELETE CASCADE
            String getUseServiceConstraintSql = 
                "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'UseService' " +
                "AND REFERENCED_TABLE_NAME = 'Service'";
            
            try {
                java.util.List<String> constraints = jdbcTemplate.queryForList(getUseServiceConstraintSql, String.class);
                for (String constraintName : constraints) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE UseService DROP FOREIGN KEY " + constraintName);
                    } catch (Exception e) {
                        System.out.println("Could not drop foreign key constraint on UseService: " + constraintName + " - " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not check/drop UseService constraints: " + e.getMessage());
            }

            try {
                jdbcTemplate.execute(
                    "ALTER TABLE UseService ADD CONSTRAINT fk_useservice_service " +
                    "FOREIGN KEY (ServiceID) REFERENCES Service(ServiceID) ON DELETE CASCADE"
                );
            } catch (Exception e) {
                // Ignore if constraint already exists
            }

            // 4. Add RoomTypeID and ServiceID columns and constraints if not exist.
            boolean hasRoomTypeCol = false;
            boolean hasServiceCol = false;
            try (ResultSet rs = metaData.getColumns(null, null, "RoomImage", null)) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    if ("RoomTypeID".equalsIgnoreCase(colName)) {
                        hasRoomTypeCol = true;
                    }
                    if ("ServiceID".equalsIgnoreCase(colName)) {
                        hasServiceCol = true;
                    }
                }
            }

            if (!hasRoomTypeCol) {
                try {
                    jdbcTemplate.execute("ALTER TABLE RoomImage ADD COLUMN RoomTypeID INT NULL");
                    jdbcTemplate.execute(
                        "ALTER TABLE RoomImage ADD CONSTRAINT fk_roomimage_roomtype " +
                        "FOREIGN KEY (RoomTypeID) REFERENCES RoomType(RoomTypeID) ON DELETE CASCADE"
                    );
                } catch (Exception e) {
                    System.out.println("Error adding RoomTypeID: " + e.getMessage());
                }
            }

            if (!hasServiceCol) {
                try {
                    jdbcTemplate.execute("ALTER TABLE RoomImage ADD COLUMN ServiceID INT NULL");
                    jdbcTemplate.execute(
                        "ALTER TABLE RoomImage ADD CONSTRAINT fk_roomimage_service " +
                        "FOREIGN KEY (ServiceID) REFERENCES Service(ServiceID) ON DELETE CASCADE"
                    );
                } catch (Exception e) {
                    System.out.println("Error adding ServiceID: " + e.getMessage());
                }
            }
            
            // 5. Migrate old status string to Enum values
            try {
                jdbcTemplate.execute("UPDATE UserAccount SET Status = 'ACTIVE' WHERE Status = 'Active' OR Status IS NULL");
                jdbcTemplate.execute("ALTER TABLE UserAccount MODIFY COLUMN Status VARCHAR(20) DEFAULT 'ACTIVE'");
            } catch (Exception e) {
                System.out.println("Error migrating UserAccount status: " + e.getMessage());
            }

            // 6. Fix inconsistent reservations and their room statuses (if an invoice is already present but reservation is not Checked-Out)
            try {
                jdbcTemplate.execute(
                    "UPDATE Room r " +
                    "JOIN ReserveDetail rd ON r.BranchID = rd.BranchID AND r.RoomNo = rd.RoomNo " +
                    "JOIN Reserve res ON rd.ReservationID = res.ReservationID " +
                    "JOIN Invoice i ON res.ReservationID = i.ReservationID " +
                    "SET r.RoomStatus = 'Đang dọn' " +
                    "WHERE res.ReservationStatus != 'Checked-Out' AND r.RoomStatus = 'Đang sử dụng'"
                );
            } catch (Exception e) {
                System.out.println("Error auto-fixing inconsistent room statuses: " + e.getMessage());
            }

            try {
                jdbcTemplate.execute(
                    "UPDATE Reserve r " +
                    "JOIN Invoice i ON r.ReservationID = i.ReservationID " +
                    "SET r.ReservationStatus = 'Checked-Out' " +
                    "WHERE r.ReservationStatus != 'Checked-Out'"
                );
            } catch (Exception e) {
                System.out.println("Error auto-fixing inconsistent reservation statuses: " + e.getMessage());
            }

            System.out.println("Database schema migration for RoomImage table completed successfully.");
        } catch (Exception e) {
            System.err.println("Database initialization schema patch failed: " + e.getMessage());
        }
    }
}
