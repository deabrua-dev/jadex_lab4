package org.example;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Task implements Serializable {
    private String name;
    private Duration duration;
    private Executor executor;
    private List<Task> prev;
    private List<Task> next;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean finished;

    public Task() {
        this.name = "Null";
        this.duration = Duration.ZERO;
        this.executor = new Executor("Null");
        this.startDate = LocalDateTime.MIN;
        this.endDate = LocalDateTime.MIN;
        this.finished = false;
        this.next = new ArrayList<>();
        this.prev = new ArrayList<>();
    }

    public Task(String name, Duration duration) {
        this.name = name;
        this.duration = duration;
        this.executor = new Executor("Null");
        this.startDate = LocalDateTime.MIN;
        this.endDate = LocalDateTime.MIN;
        this.finished = false;
        this.next = new ArrayList<>();
        this.prev = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Duration getDuration() {
        return duration;
    }

    public Executor getExecutor() {
        return executor;
    }

    public List<Task> getNext() {
        return next;
    }

    public LocalDateTime getStartDate() { return startDate; }

    public LocalDateTime getEndDate() { return endDate; }

    public List<Task> getPrev() {
        return prev;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finishTask(LocalDateTime time) {
        this.finished = true;
        this.setStartDate(time);
        this.setEndDate(time.plus(this.duration));
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void addPrevTask(Task prev) {
        this.prev.add(prev);
        prev.setAsNextTask(this);
    }

    public void addNextTask(Task next) {
        this.next.add(next);
        next.setAsPrevTask(this);
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    private void setAsPrevTask(Task prev) {
        this.prev.add(prev);
    }

    private void setAsNextTask(Task next) {
        this.next.add(next);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Task)) {
            return false;
        }
        Task task = (Task) o;

        return name.equals(task.name) && duration.equals(task.duration) &&
                executor.equals(task.executor) && prev.containsAll(task.prev) &&
                next.containsAll(task.next) && startDate.equals(task.startDate) &&
                endDate.equals(task.endDate) && finished.equals(task.finished);
    }
}
