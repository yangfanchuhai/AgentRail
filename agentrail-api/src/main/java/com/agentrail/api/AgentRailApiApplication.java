package com.agentrail.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentRail Control Plane — Definition, Registry &amp; Governance.
 *
 * <p>Manages Agent definitions (definition-state), registrations (registration-state),
 * and governance policies (governance-state).</p>
 */
@SpringBootApplication
public class AgentRailApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentRailApiApplication.class, args);
    }
}
