package com.sonhoang2.TaskManagementAPI.user.entity;

import com.sonhoang2.TaskManagementAPI.notification.entity.Notification;
import com.sonhoang2.TaskManagementAPI.project.entity.Project;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectMember;
import com.sonhoang2.TaskManagementAPI.task.entity.Task;
import com.sonhoang2.TaskManagementAPI.attachment.entity.TaskAttachment;
import com.sonhoang2.TaskManagementAPI.comment.entity.TaskComment;
import com.sonhoang2.TaskManagementAPI.history.entity.TaskHistory;
import jakarta.persistence.*;
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
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Builder.Default
    @OneToMany(mappedBy = "owner")
    private List<Project> ownedProjects = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<ProjectMember> projectMemberships = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "assignee")
    private List<Task> assignedTasks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reporter")
    private List<Task> reportedTasks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<TaskComment> taskComments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "uploader")
    private List<TaskAttachment> uploadedAttachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "changedByUser")
    private List<TaskHistory> taskHistoriesChanged = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<Notification> notifications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
