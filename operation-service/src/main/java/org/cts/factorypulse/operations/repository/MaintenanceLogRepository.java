package org.cts.factorypulse.operations.repository;

import org.cts.factorypulse.operations.model.MaintenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {
    List<MaintenanceLog> findByWorkOrderWorkOrderId(Long workOrderId);
    List<MaintenanceLog> findByWorkOrderMachineId(Long machineId);
}
