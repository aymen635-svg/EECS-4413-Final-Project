package com.laundromat.smart_laundromat.machine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class MachineService {

    private final MachineRepository repository;

    public MachineService(MachineRepository repository) {
        this.repository = repository;
    }

    public List<Machine> listActive() {
        return repository.findByActiveTrue();
    }

    public Machine get(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found" ));
    }

    @Transactional
    public Machine create(MachineRequest request) {
        if (repository.existsByMachineNumber(request.machineNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Machine number already exists" );
        }

        return repository.save(new Machine(request.machineNumber(),request.machineType(),request.status(),request.isActive()));
    }

    @Transactional
    public Machine update(Long id, MachineRequest request) {
        Machine machine = get(id);

        if (!machine.getMachineNumber().equals(request.machineNumber()) && repository.existsByMachineNumber(request.machineNumber())) {
            throw new ResponseStatusException( HttpStatus.CONFLICT, "Machine number already exists");
        }

        machine.setMachineNumber(request.machineNumber());
        machine.setMachineType(request.machineType());
        machine.setStatus(request.status());
        machine.setActive(request.isActive());

        return machine;
    }

    @Transactional
    public Machine changeStatus(Long id, Machine.MachineStatus status) {
        Machine machine = get(id);
        machine.setStatus(status);
        return machine;
    }

    @Transactional
    public void deactivate(Long id) {
        get(id).setActive(false);
    }
}