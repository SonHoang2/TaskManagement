package com.sonhoang2.project_service.project.entity;

import com.sonhoang2.TaskManagementAPI.label.entity.Label;
import com.sonhoang2.TaskManagementAPI.sprint.entity.Sprint;
import com.sonhoang2.TaskManagementAPI.task.entity.Task;
import com.sonhoang2.TaskManagementAPI.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Lob
    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @Builder.Default
    @OneToMany(mappedBy = "project")
    private List<ProjectMember> members = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "project")
    private List<Task> tasks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "project")
    private List<Label> labels = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "project")
    private List<Sprint> sprints = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}

