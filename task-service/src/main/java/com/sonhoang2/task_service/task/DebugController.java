package com.sonhoang2.task_service.task;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    private final TaskLeakHolder leakHolder;

    public DebugController(TaskLeakHolder leakHolder) {
        this.leakHolder = leakHolder;
    }

    @GetMapping("/debug/leak-size")
    public int leakSize() {
        return leakHolder.size();
    }
}