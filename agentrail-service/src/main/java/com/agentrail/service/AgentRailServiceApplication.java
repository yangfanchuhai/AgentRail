package com.agentrail.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentRail Data Plane — Runtime, Run Lifecycle &amp; Temporal Workflows.
 *
 * <p>Serves as the runtime entry point. Manages Run lifecycle (running-state),
 * integrates with Temporal for workflow orchestration.</p>
 */
@SpringBootApplication
public class AgentRailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentRailServiceApplication.class, args);
    }
}
