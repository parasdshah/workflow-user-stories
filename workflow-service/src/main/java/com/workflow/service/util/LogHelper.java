package com.workflow.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogHelper {
    private static final Logger log = LoggerFactory.getLogger(LogHelper.class);

    public static void logGroupQuery(String groupId, int count) {
        log.info("Group Workload Query - Group: {}, Found Tasks: {}", groupId, count);
    }
}
