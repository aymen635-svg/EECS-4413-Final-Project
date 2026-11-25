package com.laundromat.config;

import com.laundromat.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AuthService authService;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(AuthService authService,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword) {
        this.authService = authService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        authService.createAdminIfMissing(adminEmail, adminPassword);
    }
}
