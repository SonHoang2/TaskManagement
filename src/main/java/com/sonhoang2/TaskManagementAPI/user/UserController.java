package com.sonhoang2.TaskManagementAPI.user;

import com.sonhoang2.TaskManagementAPI.common.dto.JSendResponse;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.user.dto.UserCreateRequest;
import com.sonhoang2.TaskManagementAPI.user.dto.UserResponse;
import com.sonhoang2.TaskManagementAPI.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, UserResponse>>> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse createdUser = userService.create(request);
        return ResponseEntity.created(URI.create("/users/" + createdUser.getId()))
                .body(JSendResponse.success(Map.of("user", createdUser)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<UserResponse>>>> findAll(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        PageResponse<UserResponse> pageResponse = userService.findAll(keyword, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, UserResponse>>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("user", userService.findById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, UserResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("user", userService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}

