package org.cts.factorypulse.operations.repository;

import org.cts.factorypulse.operations.model.RootCause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RootCauseRepository extends JpaRepository<RootCause, Long> {
    List<RootCause> findByCategory(String category);
}
