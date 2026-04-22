package org.cts.factorypulse.operations.repository;

import org.cts.factorypulse.operations.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByStatus(String status);
    List<WorkOrder> findByMachineId(Long machineId);
    List<WorkOrder> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<WorkOrder> findByAssignedToId(Long userId);
    List<WorkOrder> findByAssignedToIdAndStatusIn(Long userId, List<String> statuses);
    boolean existsByDowntimeEventDowntimeId(Long downtimeId);
}
