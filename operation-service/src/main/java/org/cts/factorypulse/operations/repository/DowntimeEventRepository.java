package org.cts.factorypulse.operations.repository;

import org.cts.factorypulse.operations.model.DowntimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, Long> {
    List<DowntimeEvent> findByLineIdAndStartAtBetweenOrderByStartAtDesc(Long lineId, LocalDateTime from, LocalDateTime to);
    List<DowntimeEvent> findByMachineIdAndStartAtBetweenOrderByStartAtDesc(Long machineId, LocalDateTime from, LocalDateTime to);
    List<DowntimeEvent> findByEndAtIsNull();
    List<DowntimeEvent> findByStartAtBetween(LocalDateTime from, LocalDateTime to);
}
