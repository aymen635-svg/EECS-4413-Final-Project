package com.laundromat.config;

import com.laundromat.service.AuthService;
import com.laundromat.model.Machine;
import com.laundromat.repository.MachineRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AuthService authService;
    private final MachineRepository machineRepository;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(AuthService authService,
            MachineRepository machineRepository,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {
        this.authService = authService;
        this.machineRepository = machineRepository;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        authService.createAdminIfMissing(adminEmail, adminPassword);

        seedMachine("W001", "WASHER");
        seedMachine("W002", "WASHER");
        seedMachine("W003", "WASHER");
        seedMachine("W004", "WASHER");

        seedMachine("D001", "DRYER");
        seedMachine("D002", "DRYER");
        seedMachine("D003", "DRYER");

        System.out.println("Machine seeding completed.");
    }

    private void seedMachine(String number, String type) {
        if (!machineRepository.existsByMachineNumber(number)) {
            machineRepository.save(new Machine(number, type));
        }
    }
}
