package org.example;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Project implements Serializable {
    private String name;
    private Task firstTask;
    private Task lastTask;
    private List<Executor> executors;
    private LocalDateTime startDate;
    private Duration plannedDuration;
    private Duration minimalDuration;
    private Boolean isUpdated;

    public Project(String name, LocalDateTime startDate, Duration plannedDuration) {
        this.name = name;
        this.startDate = startDate;
        this.firstTask = new Task();
        this.lastTask = new Task();
        this.executors = new ArrayList<>();
        this.minimalDuration = Duration.ZERO;
        this.plannedDuration = plannedDuration;
        this.isUpdated = false;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getStartDate() { return this.startDate; }

    public LocalDateTime getEndDate() { return this.lastTask.getEndDate(); }

    public List<Executor> getExecutors() {
        return this.executors;
    }

    public void setExecutors(List<Executor> executors) {
        this.executors = new ArrayList<>(executors);
        this.isUpdated = false;
    }

    // Функція встановлює перше завдання проекту та визначає останнє
    // У проекта може бути лише одне перше і останнє завдання
    public void addFirstTask(Task task) {
        this.firstTask = task;
        Task temp = this.firstTask.getNext().get(0);
        while (!temp.getNext().isEmpty()) {
            this.lastTask = temp.getNext().get(0);
            temp = this.lastTask;
        }
        this.isUpdated = false;
    }

    // Виводимо мінімальний час виконнання проекту
    public Duration getMinimalDuration() throws Exception {
        if (!this.isUpdated) {
            body();
        }
        return this.minimalDuration;
    }

    // Виводимо розклад виконання кожного завдання та його виконавця
    public List<Task> getProjectSchedule() throws Exception {
        if (!this.isUpdated) {
            body();
        }
        return getListOfUniqueElem();
    }

    // Виводимо список завдань кожного із виконавців
    public List<Executor> getExecutorsSchedule() throws Exception {
        if (!this.isUpdated) {
            body();
        }
        return this.executors;
    }

    // Отримуємо необхідну кількість виконавців для того, щоб встигнути виконати проект у заплановані терміни
    public AbstractMap.SimpleEntry<Integer, Duration> getMinimalRequiredExecutorsNumber() throws Exception {
        if (!this.isUpdated) {
            body();
        }
        AbstractMap.SimpleEntry<Integer, Duration> result = new AbstractMap.SimpleEntry<>(this.executors.size(), this.minimalDuration);
        List<Executor> temp = new ArrayList<>(this.executors);
        boolean compareWithPlanned = this.minimalDuration.compareTo(this.plannedDuration) < 0;
        if (compareWithPlanned) {
            System.out.println("The required number of executors: " + this.executors.size());
        } else {
            int i = 1;
            int streak = 0;
            Duration prev = Duration.ZERO;
            while (!compareWithPlanned && streak < 5) {
                this.executors.add(new Executor("Temp " + i++));
                prev = this.minimalDuration;
                body();
                boolean compareWithPrev = this.minimalDuration.compareTo(prev) == 0;
                if (compareWithPrev) {
                    streak++;
                } else {
                    streak = 0;
                }
                compareWithPlanned = this.minimalDuration.compareTo(this.plannedDuration) < 0;
            }
            if (!compareWithPlanned) {
                throw new Exception("Can`t calculate required executors number.");
            }
            result = new AbstractMap.SimpleEntry<>(this.executors.size() - streak - temp.size(), this.minimalDuration);
            this.executors = temp;
            body();
        }
        return result;
    }

    // Алгоритм створює графік виконання завдань та розподіляє виконавців по ним
    private void body() throws Exception {
        if (this.executors.isEmpty()) {
            throw new Exception("Executors is not set.");
        } else if (this.firstTask.getName().equals("Null")) {
            throw new Exception("Fist task is not set.");
        } else {
            for (Executor executor : this.executors) {
                executor.clearTasks();
            }
            Set<Task> set = new HashSet<>();
            Deque<Task> deque = new ArrayDeque<>();
            deque.add(this.firstTask);
            set.add(this.firstTask);
            Task task = new Task();
            for (Executor executor : this.executors) {
                executor.setEndTaskAt(this.startDate);
            }
            Deque<Executor> executorsDeque = new ArrayDeque<>(this.executors);
            while (!deque.isEmpty()) {
                task = deque.pop();
                List<Task> prev = task.getPrev();
                List<Task> next = task.getNext();
                if ((prev.isEmpty() || prev.stream().allMatch(Task::isFinished)) && !executorsDeque.isEmpty()) {
                    Executor executor = executorsDeque.pop();
                    LocalDateTime time = this.startDate;
                    if (!prev.isEmpty()) {
                        time = prev.stream().max(Comparator.comparing(Task::getDuration)).get().getEndDate();
                    }
                    if (!time.isAfter(executor.getEndTaskAt())) {
                        time = executor.getEndTaskAt();
                    }
                    task.finishTask(time);
                    task.setExecutor(executor);
                    executor.addTask(task);
                    executor.setEndTaskAt(task.getEndDate());
                    for (Task value : next) {
                        if (!set.contains(value)) {
                            set.add(value);
                            deque.add(value);
                        }
                    }
                } else if (executorsDeque.isEmpty() && prev.stream().allMatch(Task::isFinished)) {
                    for (Task value : prev) {
                        if (!executorsDeque.contains(value.getExecutor())) {
                            executorsDeque.add(value.getExecutor());
                        }
                    }
                    deque.push(task);
                } else {
                    deque.add(deque.pop());
                }
            }
            this.minimalDuration = Duration.between(getStartDate(), getEndDate());
            this.isUpdated = true;
        }
    }

    // Отримуємо список всіх завдань у проекті
    private List<Task> getListOfUniqueElem() {
        Deque<Task> deque = new ArrayDeque<>();
        List<Task> list = new ArrayList<>();
        deque.add(this.firstTask);
        list.add(this.firstTask);
        while (!deque.isEmpty()) {
            Task popTask = deque.pop();
            List<Task> next = popTask.getNext();
            if (next.isEmpty()) {
                break;
            }
            for (Task nextTask : next) {
                if (!list.contains(nextTask) && list.containsAll(nextTask.getPrev())) {
                    deque.add(nextTask);
                    list.add(nextTask);
                }
            }
        }
        return list;
    }

    // Порівння фактичного часу виконання проекту зі запланованим
    public boolean comparePlannedDurationWithActual() throws Exception {
        if (!this.isUpdated) {
            body();
        }
        return this.minimalDuration.compareTo(this.plannedDuration) < 0 && this.minimalDuration != Duration.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Project)) {
            return false;
        }
        Project project = (Project) o;
        boolean exe = executors.containsAll(project.executors);
        return name.equals(project.name) && firstTask.equals(project.firstTask) &&
                lastTask.equals(project.lastTask) && executors.containsAll(project.executors) &&
                startDate.equals(project.startDate) && plannedDuration.equals(project.plannedDuration) &&
                minimalDuration.equals(project.minimalDuration) && isUpdated.equals(project.isUpdated);
    }
}
