package com.laundromat.smart_laundromat.reservation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    // Create reservation
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {

        Reservation reservation = service.create(request);
        ReservationResponse response = new ReservationResponse(reservation);
        return ResponseEntity.status(201).body(response);
    }

    // Cancel reservation
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable Long id) {

        Reservation reservation = service.cancel(id);

        return ResponseEntity.ok(
            new ReservationResponse(reservation)
        );
    }

    // Get reservation by ID
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> get(
            @PathVariable Long id) {
            Reservation reservation = service.getOwned(id);

           return ResponseEntity.ok(new ReservationResponse(reservation)
        );
    }

     // Get logged-in customer's reservations
     @GetMapping
     public ResponseEntity<List<ReservationResponse>> mine() {
 
         List<ReservationResponse> responses = service.mine().stream().map(ReservationResponse::new).toList();
         return ResponseEntity.ok(responses);
     }
}

