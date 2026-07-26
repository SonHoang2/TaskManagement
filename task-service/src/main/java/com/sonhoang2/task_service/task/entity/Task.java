package com.sonhoang2.task_service.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonhoang2.task_service.attachment.entity.TaskAttachment;
import com.sonhoang2.task_service.comment.entity.TaskComment;
import com.sonhoang2.task_service.history.entity.TaskHistory;
import com.sonhoang2.task_service.tasklabel.entity.TaskLabel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TaskPriority priority;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "parent_task_id")
    private UUID parentTaskId;

    @ManyToOne
    @JoinColumn(name = "parent_task_id", insertable = false, updatable = false)
    @JsonIgnore
    private Task parentTask;

    @Builder.Default
    @OneToMany(mappedBy = "parentTask")
    private List<Task> subtasks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "task")
    private List<TaskComment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "task")
    private List<TaskAttachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "task")
    private List<TaskHistory> histories = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "task")
    private List<TaskLabel> taskLabels = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}