package org.cts.factorypulse.operations.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CorrectiveActionRequest {
    @NotNull(message = "Downtime ID is required") private Long downtimeId;
    @NotNull(message = "Assigned user ID is required") private Long assignedTo;
    @NotBlank(message = "Description is required") private String description;
    @NotNull(message = "Due date is required") private LocalDate dueDate;
}
