package org.cts.factorypulse.operations.repository;

import org.cts.factorypulse.operations.model.ProductionCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionCountRepository extends JpaRepository<ProductionCount, Long> {
    List<ProductionCount> findByLineIdAndTimeStampBetweenOrderByTimeStampDesc(Long lineId, LocalDateTime from, LocalDateTime to);
    List<ProductionCount> findByShiftId(Long shiftId);
    List<ProductionCount> findByTimeStampBetween(LocalDateTime from, LocalDateTime to);
}
