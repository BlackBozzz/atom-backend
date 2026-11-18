package com.atomskills.atombackend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdificeDependencyBatchTask {
    private List<EdificeDependencyTask> items;
}
