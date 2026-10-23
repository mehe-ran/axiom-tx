package com.axiomtx.reconciliation;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ReconciliationApplication {

    public static void main(String[] args) {
        int exitCode;
        try (var context = new SpringApplicationBuilder(ReconciliationApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            exitCode = context.getBean(LedgerAudit.class).run();
        }
        System.exit(exitCode);
    }
}
