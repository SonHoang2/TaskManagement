package com.sonhoang2.task_service.tasklabel.entity;

import com.sonhoang2.task_service.task.entity.Task;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "task_labels", uniqueConstraints = {@UniqueConstraint(columnNames = {"task_id", "label_id"})})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;
}


