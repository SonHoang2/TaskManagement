package com.sonhoang2.sprint_service.sprint.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "task_sprints")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSprint {

    @EmbeddedId
    private TaskSprintId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sprintId")
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;
}