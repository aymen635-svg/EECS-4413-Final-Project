package com.laundromat.smart_laundromat.machine;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machines")
public class MachineController {

    private final MachineService service;

    public MachineController(MachineService service) {
        this.service = service;
    }

    @GetMapping
    public List<Machine> list() {
        return service.listActive();
    }

    @GetMapping("/{id}")
    public Machine get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Machine create(
            @Valid @RequestBody MachineRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Machine update(
            @PathVariable Long id,
            @Valid @RequestBody MachineRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public Machine changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody MachineStatusRequest request) {
        return service.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}

