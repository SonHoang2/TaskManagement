package com.sonhoang2.project_service.label;

import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.project_service.label.dto.LabelCreateRequest;
import com.sonhoang2.project_service.label.dto.LabelRepository;
import com.sonhoang2.project_service.label.dto.LabelResponse;
import com.sonhoang2.project_service.label.dto.LabelUpdateRequest;
import com.sonhoang2.project_service.label.entity.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceImplTest {

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private LabelServiceImpl labelService;

    private UUID labelId;
    private UUID projectId;
    private Label label;
    private LabelCreateRequest createRequest;
    private LabelUpdateRequest updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        labelId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        label = Label.builder()
                .id(labelId)
                .projectId(projectId)
                .name("Test Label")
                .color("#FF0000")
                .createdAt(Instant.now())
                .build();

        createRequest = LabelCreateRequest.builder()
                .projectId(projectId)
                .name("Test Label")
                .color("#FF0000")
                .build();

        updateRequest = LabelUpdateRequest.builder()
                .projectId(projectId)
                .name("Updated Label")
                .color("#00FF00")
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void create_ShouldReturnLabelResponse() {
        // Arrange
        when(labelRepository.save(any(Label.class))).thenReturn(label);

        // Act
        LabelResponse result = labelService.create(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(labelId, result.getId());
        assertEquals(projectId, result.getProjectId());
        assertEquals("Test Label", result.getName());
        assertEquals("#FF0000", result.getColor());
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void findAll_WithProjectIdAndName_ShouldReturnFilteredPage() {
        // Arrange
        Page<Label> page = new PageImpl<>(List.of(label), pageable, 1); // Thêm pageable và totalElements
        when(labelRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, "Test", pageable))
                .thenReturn(page);

        // Act
        PageResponse<LabelResponse> result = labelService.findAll(projectId, "Test", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(labelId, result.content().get(0).getId());
        assertEquals(0, result.page());
        assertEquals(10, result.size()); // Bây giờ sẽ là 10
        assertEquals(1, result.totalElements());
        verify(labelRepository, times(1))
                .findByProjectIdAndNameContainingIgnoreCase(projectId, "Test", pageable);
    }

    @Test
    void findAll_WithProjectIdOnly_ShouldReturnFilteredPage() {
        // Arrange
        Page<Label> page = new PageImpl<>(List.of(label));
        when(labelRepository.findByProjectId(projectId, pageable)).thenReturn(page);

        // Act
        PageResponse<LabelResponse> result = labelService.findAll(projectId, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(labelId, result.content().get(0).getId());
        verify(labelRepository, times(1)).findByProjectId(projectId, pageable);
    }

    @Test
    void findAll_WithNameOnly_ShouldReturnFilteredPage() {
        // Arrange
        Page<Label> page = new PageImpl<>(List.of(label));
        when(labelRepository.findByNameContainingIgnoreCase("Test", pageable)).thenReturn(page);

        // Act
        PageResponse<LabelResponse> result = labelService.findAll(null, "Test", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(labelId, result.content().get(0).getId());
        verify(labelRepository, times(1)).findByNameContainingIgnoreCase("Test", pageable);
    }

    @Test
    void findAll_WithNoFilters_ShouldReturnAllLabels() {
        // Arrange
        Page<Label> page = new PageImpl<>(List.of(label));
        when(labelRepository.findAll(pageable)).thenReturn(page);

        // Act
        PageResponse<LabelResponse> result = labelService.findAll(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(labelId, result.content().get(0).getId());
        verify(labelRepository, times(1)).findAll(pageable);
    }

    @Test
    void findById_ShouldReturnLabelResponse_WhenLabelExists() {
        // Arrange
        when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));

        // Act
        LabelResponse result = labelService.findById(labelId);

        // Assert
        assertNotNull(result);
        assertEquals(labelId, result.getId());
        assertEquals(projectId, result.getProjectId());
        assertEquals("Test Label", result.getName());
        assertEquals("#FF0000", result.getColor());
        verify(labelRepository, times(1)).findById(labelId);
    }

    @Test
    void findById_ShouldThrowException_WhenLabelNotFound() {
        // Arrange
        when(labelRepository.findById(labelId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> labelService.findById(labelId));
        verify(labelRepository, times(1)).findById(labelId);
    }

    @Test
    void update_ShouldReturnUpdatedLabelResponse() {
        // Arrange
        Label updatedLabel = Label.builder()
                .id(labelId)
                .projectId(projectId)
                .name("Updated Label")
                .color("#00FF00")
                .createdAt(Instant.now())
                .build();

        when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));
        doNothing().when(modelMapper).map(updateRequest, label);
        when(labelRepository.save(any(Label.class))).thenReturn(updatedLabel);

        // Act
        LabelResponse result = labelService.update(labelId, updateRequest);

        // Assert
        assertNotNull(result);
        verify(labelRepository, times(1)).findById(labelId);
        verify(modelMapper, times(1)).map(updateRequest, label);
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void update_ShouldThrowException_WhenLabelNotFound() {
        // Arrange
        when(labelRepository.findById(labelId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> labelService.update(labelId, updateRequest));
        verify(labelRepository, times(1)).findById(labelId);
        verify(modelMapper, never()).map(any(), any());
        verify(labelRepository, never()).save(any());
    }

    @Test
    void delete_ShouldDeleteLabel_WhenLabelExists() {
        // Arrange
        when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));
        doNothing().when(labelRepository).delete(label);

        // Act
        labelService.delete(labelId);

        // Assert
        verify(labelRepository, times(1)).findById(labelId);
        verify(labelRepository, times(1)).delete(label);
    }

    @Test
    void delete_ShouldThrowException_WhenLabelNotFound() {
        // Arrange
        when(labelRepository.findById(labelId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> labelService.delete(labelId));
        verify(labelRepository, times(1)).findById(labelId);
        verify(labelRepository, never()).delete(any());
    }

    @Test
    void findAll_EmptyPage_ShouldReturnEmptyResponse() {
        // Arrange
        Page<Label> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(labelRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        PageResponse<LabelResponse> result = labelService.findAll(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());
    }

    @Test
    void findAll_MultiplePages_ShouldReturnCorrectPaginationInfo() {
        // Arrange
        List<Label> labels = List.of(
                Label.builder()
                        .id(UUID.randomUUID())
                        .projectId(projectId)
                        .name("Label 1")
                        .color("#FF0000")
                        .createdAt(Instant.now())
                        .build(),
                Label.builder()
                        .id(UUID.randomUUID())
                        .projectId(projectId)
                        .name("Label 2")
                        .color("#00FF00")
                        .createdAt(Instant.now())
                        .build(),
                Label.builder()
                        .id(UUID.randomUUID())
                        .projectId(projectId)
                        .name("Label 3")
                        .color("#0000FF")
                        .createdAt(Instant.now())
                        .build()
        );
        Page<Label> page = new PageImpl<>(labels, pageable, 10);
        when(labelRepository.findAll(pageable)).thenReturn(page);

        // Act
        PageResponse<LabelResponse> result = labelService.findAll(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.content().size());
        assertEquals(10, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());
    }
}