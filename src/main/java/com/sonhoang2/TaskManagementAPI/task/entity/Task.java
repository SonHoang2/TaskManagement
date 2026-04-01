package com.sonhoang2.TaskManagementAPI.task.entity;

import com.sonhoang2.TaskManagementAPI.project.entity.Project;
import com.sonhoang2.TaskManagementAPI.user.entity.User;
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

    @ManyToOne
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
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

    @ManyToOne
    @JoinColumn(name = "assignee_id", insertable = false, updatable = false)
    private User assignee;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @ManyToOne
    @JoinColumn(name = "reporter_id", insertable = false, updatable = false)
    private User reporter;

    private Instant dueDate;

    private Instant startDate;

    @Column(name = "parent_task_id")
    private UUID parentTaskId;

    @ManyToOne
    @JoinColumn(name = "parent_task_id", insertable = false, updatable = false)
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

    @Builder.Default
    @OneToMany(mappedBy = "task")
    private List<TaskSprint> taskSprints = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
