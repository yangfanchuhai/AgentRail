package com.agentrail.service.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal Client configuration for agentrail-service.
 * Service module only creates Temporal Client (not Worker).
 * The Python worker registers and executes workflows/activities.
 */
@Slf4j
@Configuration
public class TemporalConfig {

    @Value("${temporal.connection.target:localhost:7233}")
    private String target;

    @Value("${temporal.connection.namespace:agentrail}")
    private String namespace;

    @Value("${temporal.connection.enable-https:false}")
    private boolean enableHttps;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("Connecting to Temporal at {} (namespace={})", target, namespace);
        var stubsOptions = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(target)
                .setEnableHttps(enableHttps)
                .build();
        return WorkflowServiceStubs.newServiceStubs(stubsOptions);
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        var clientOptions = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .build();
        return WorkflowClient.newInstance(stubs, clientOptions);
    }
}
