package com.sonhoang2.sprint_service.sprint.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TaskSprintId implements Serializable {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;
}


