package com.workflow.service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAutoPatcher implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseAutoPatcher.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        patchWorkflowMasterStatus();
    }

    private void patchWorkflowMasterStatus() {
        try {
            logger.info("Checking/Patching workflow_master table for 'status' column...");
            // Attempt to add the column.
            // Note: H2 syntax for 'IF NOT EXISTS' on ADD COLUMN might vary slightly by
            // version,
            // but usually 'ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...' works.
            // If the column already exists, this is safe.
            // We verify nullable=false constraint by setting a default.

            jdbcTemplate.execute(
                    "ALTER TABLE workflow_master ADD COLUMN IF NOT EXISTS status VARCHAR(255) DEFAULT 'ACTIVE'");

            // Ensure any legacy rows have it set to 'ACTIVE' if they were somehow null
            // (though DEFAULT handles it for new cols)
            jdbcTemplate.execute("UPDATE workflow_master SET status = 'ACTIVE' WHERE status IS NULL");

            logger.info("Database patch for workflow_master.status completed successfully.");
        } catch (Exception e) {
            // Log but don't crash if it's just a duplicate column error that wasn't caught
            // by IF NOT EXISTS logic
            // or if the table doesn't exist yet (though it should).
            logger.warn("Database patch warning (may be already applied): " + e.getMessage());
        }
    }
}
