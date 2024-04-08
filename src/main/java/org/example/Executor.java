package org.example;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Executor implements Serializable {
    private String name;
    private List<Task> taskList;
    private LocalDateTime endTaskAt;

    public Executor(String name) {
        this.name = name;
        this.endTaskAt = LocalDateTime.MIN;
        this.taskList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getEndTaskAt() {
        return endTaskAt;
    }

    public void setEndTaskAt(LocalDateTime endTaskAt) {
        this.endTaskAt = endTaskAt;
    }

    public void addTask(Task task) {
        this.taskList.add(task);
    }

    public void clearTasks() {
        this.taskList.clear();
        this.endTaskAt = LocalDateTime.MIN;
    }

    public void printTasks() {
        System.out.println("\nExecutor name: " + this.name + " ");
        for (int i = 0; i < this.taskList.size(); i++) {
            System.out.println("Task name: " + this.taskList.get(i).getName());
            System.out.println("Start time: " + this.taskList.get(i).getStartDate());
            System.out.println("End time: " + this.taskList.get(i).getEndDate());
        }
        System.out.println();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Executor)) {
            return false;
        }
        Executor executor = (Executor) o;

        return name.equals(executor.name) && taskList.containsAll(executor.taskList) &&
                endTaskAt.equals(executor.endTaskAt);
    }
}
