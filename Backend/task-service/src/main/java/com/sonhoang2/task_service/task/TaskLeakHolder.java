package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.task.entity.Task;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TaskLeakHolder {
    // static => lives for the whole JVM lifetime, the GC can never reclaim it
    // even when nobody "needs" this list anymore
    private static final List<Task> LEAKED_TASKS = new ArrayList<>();

    public void hold(Task task) {
        LEAKED_TASKS.add(task);
    }

    public int size() {
        return LEAKED_TASKS.size();
    }
}