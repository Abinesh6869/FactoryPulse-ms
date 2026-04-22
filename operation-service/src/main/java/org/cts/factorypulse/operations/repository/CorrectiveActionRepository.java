package org.cts.factorypulse.operations.repository;

import org.cts.factorypulse.operations.model.CorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    List<CorrectiveAction> findByDowntimeEventDowntimeId(Long downtimeId);
}
