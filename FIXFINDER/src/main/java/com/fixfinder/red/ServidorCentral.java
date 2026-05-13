package com.fixfinder.red;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Servidor Central de FixFinder.
 * Migrado de Sockets TCP puros a Spring Boot Application.
 */
@SpringBootApplication(scanBasePackages = "com.fixfinder")
@EntityScan(basePackages = "com.fixfinder.modelos")
@EnableJpaRepositories(basePackages = "com.fixfinder.repository")
public class ServidorCentral {

    public static void main(String[] args) {
        SpringApplication.run(ServidorCentral.class, args);
        System.out.println("🚀 Servidor FIXFINDER (Spring Boot) Iniciado!");
    }
}
