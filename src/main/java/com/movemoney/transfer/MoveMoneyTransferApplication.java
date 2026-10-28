package com.movemoney.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Move Money Payment Rails Modernization microservice.
 * Modernized from legacy mainframe COBOL routine (XFERPROC.cbl).
 */
@SpringBootApplication
public class MoveMoneyTransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoveMoneyTransferApplication.class, args);
    }
}
