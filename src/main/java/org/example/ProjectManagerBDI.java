package org.example;

import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.commons.future.IFuture;
import jadex.commons.transformation.annotations.Classname;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.AgentFeature;
import jadex.rules.eca.ChangeInfo;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Agent
public class ProjectManagerBDI {
    @AgentFeature
    protected IExecutionFeature execFeature;
    @AgentFeature
    protected IBDIAgentFeature bdiFeature;
    @Belief
    protected List<Project> projectsList;
    @Belief
    protected List<Executor> executorsList;

    protected static int SERVER_PORT = 5000;
    protected static int OTHER_AGENT_PORT = 9000;

    private boolean isAcceptedChanges = false;
    private boolean isInit = false;

    @AgentCreated
    public void init() {
        this.projectsList = new ArrayList<>();
        this.executorsList = new ArrayList<>();
        Runnable run = () -> {
            try {
                ServerSocket server = new ServerSocket(SERVER_PORT);
                while (true) {
                    final Socket clientSocket = server.accept();
                    execFeature.scheduleStep(new IComponentStep<Void>() {
                        @Classname("acceptdatagoal")
                        public IFuture<Void> execute(IInternalAccess ia) {
                            bdiFeature.dispatchTopLevelGoal(new AcceptDataGoal(clientSocket)).get();
                            return IFuture.DONE;
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };
        Thread t = new Thread(run);
        t.start();
    }

    @AgentBody
    public void body() {
        bdiFeature.adoptPlan("initializeTestProject").get();
        while (true) {
            try {
                System.out.println("\nChoose a method to execute: ");
                System.out.println("1. Print minimal project duration.");
                System.out.println("2. Print project task schedule.");
                System.out.println("3. Print executors task schedule.");
                System.out.println("4. Check project planned terms.");
                System.out.println("5. Get minimal required number of executors.");
                System.out.println("6. Add new project.");
                System.out.println("7. Change task.");
                System.out.println("8. Exit.");

                System.out.print("Enter a code: ");
                Scanner in = new Scanner(System.in);
                int input = in.nextInt();
                if (input == 1) {
                    for (Project project : this.projectsList) {
                        System.out.println("Project: " + project.getName());
                        bdiFeature.adoptPlan("printProjectMinimalDuration", project).get();
                    }
                } else if (input == 2) {
                    for (Project project : this.projectsList) {
                        System.out.println("Project: " + project.getName());
                        bdiFeature.adoptPlan("printScheduleProject", project).get();
                    }
                } else if (input == 3) {
                    for (Project project : this.projectsList) {
                        System.out.println("Project: " + project.getName());
                        bdiFeature.adoptPlan("printExecutorsSchedule", project).get();
                    }
                } else if (input == 4) {
                    for (Project project : this.projectsList) {
                        System.out.println("Project: " + project.getName());
                        bdiFeature.adoptPlan("printComparePlannedDurationWithActual", project).get();
                    }
                } else if (input == 5) {
                    for (Project project : this.projectsList) {
                        System.out.println("Project: " + project.getName());
                        bdiFeature.dispatchTopLevelGoal(new GetRequiredExecutorsNumber(project)).get();
                    }
                } else if (input == 6) {
                    projectsList.add(new Project("Temp", LocalDateTime.now(), Duration.ZERO));
                } else if (input == 7) {
                    projectsList.get(0).addFirstTask(new Task());
                } else if (input == 8) {
                    break;
                } else {
                    throw new Exception("Input argument is not valid!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Plan
    protected void initializeTestProject() {
        Executor executor1 = new Executor("Mike");
        Executor executor2 = new Executor("Jerry");
        Executor executor3 = new Executor("Tom");

        this.executorsList.add(executor1);
//        this.executorsList.add(executor2);
//        this.executorsList.add(executor3);

        Project project1 = new Project("Test project!", LocalDateTime.now(), Duration.ofHours(1));

        project1.setExecutors(this.executorsList);

        Task task1 = new Task("Task1", Duration.ofHours(1));
        Task task210 = new Task("Task2.1", Duration.ofHours(2));
        Task task211 = new Task("Task2.1.1", Duration.ofHours(1));
        Task task212 = new Task("Task2.1.2", Duration.ofHours(1));
        Task task220 = new Task("Task2.2", Duration.ofHours(1));
        Task task221 = new Task("Task2.2.1", Duration.ofHours(3));
        Task task23 = new Task("Task2.3", Duration.ofHours(1));
        Task task231 = new Task("Task2.3.1", Duration.ofHours(4));
        Task task31 = new Task("Task3.1", Duration.ofHours(1));
        Task task32 = new Task("Task3.2", Duration.ofHours(2));
        Task task4 = new Task("Task4", Duration.ofHours(1));

        task1.addNextTask(task210);
        task1.addNextTask(task220);
        task1.addNextTask(task23);

        task210.addNextTask(task211);
        task220.addNextTask(task221);
        task211.addNextTask(task212);

        task212.addNextTask(task31);
        task221.addNextTask(task31);
        task231.addNextTask(task31);
        task23.addNextTask(task231);

        task212.addNextTask(task32);
        task221.addNextTask(task32);
        task231.addNextTask(task32);

        task31.addNextTask(task4);
        task32.addNextTask(task4);

        project1.addFirstTask(task1);
        isInit = true;
        projectsList.add(project1);
    }

    @Plan
    public void printProjectMinimalDuration(ChangeEvent<Object[]> event) {
        try {
            Project currentProject = (Project) event.getValue()[0];
            Duration minimalDuration = currentProject.getMinimalDuration();
            System.out.println("\nProject minimal duration: " + minimalDuration);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan
    public void printScheduleProject(ChangeEvent<Object[]> event) {
        try {
            Project currentProject = (Project) event.getValue()[0];
            List<Task> taskList = currentProject.getProjectSchedule();
            int i = 1;
            for (Task task : taskList) {
                System.out.println("\n" + i++ + ") Task name: " + task.getName() + "\nExecutor: " + task.getExecutor().getName() +
                        "\nStart time: " + task.getStartDate() + "\nEnd time: " + task.getEndDate());
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan
    public void printExecutorsSchedule(ChangeEvent<Object[]> event) {
        try {
            Project currentProject = (Project) event.getValue()[0];
            List<Executor> projectExecutors = currentProject.getExecutorsSchedule();
            for (Executor executor : projectExecutors) {
                executor.printTasks();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan
    public void printComparePlannedDurationWithActual(ChangeEvent<Object[]> event) {
        try {
            Project currentProject = (Project) event.getValue()[0];
            boolean result = currentProject.comparePlannedDurationWithActual();
            if (result) {
                System.out.println("\nThe project can be completed in time.");
            } else {
                System.out.println("\nThe project cannot be completed in time.");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan(trigger = @Trigger(goals = GetRequiredExecutorsNumber.class))
    public void getRequiredExecutorsNumber(GetRequiredExecutorsNumber goal) {
        try {
            Project currentProject = goal.getCurrentProject();
            AbstractMap.SimpleEntry<Integer, Duration> result = currentProject.getMinimalRequiredExecutorsNumber();
            goal.setRequiredExecutorsNumber(result.getKey());
            goal.setExpectedDuration(result.getValue());
            System.out.println("\nThe required number of new executors: " + goal.getRequiredExecutorsNumber());
            System.out.println("Expected project duration: " + goal.getExpectedDuration());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan(trigger = @Trigger(goals = AcceptDataGoal.class))
    public void acceptData(AcceptDataGoal goal) {
        try {
            Socket client = goal.getClient();
            System.out.println("Accept new data.");
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            List<Project> acceptedProjects = (List<Project>) in.readObject();
            if (acceptedProjects.isEmpty()) {
                throw new Exception("Accepted null");
            }
            for (Project project : acceptedProjects) {
                if (projectsList.stream().anyMatch(i -> i.getName().equals(project.getName()) && !i.equals(project))) {
                    for (int i = 0; i < projectsList.size(); i++) {
                        if (projectsList.get(i).getName().equals(project.getName())) {
                            projectsList.set(i, project);
                            isAcceptedChanges = true;
                            break;
                        }
                    }
                } else if (!projectsList.stream().anyMatch(i -> i.equals(project))) {
                    projectsList.add(project);
                    isAcceptedChanges = true;
                }
            }
            System.out.println("The data was successfully accepted.");
            System.out.println("Please repeat your request.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan(trigger = @Trigger(factchangeds = "projectsList"))
    public void sendNewProjectChanged(ChangeEvent event) {
        try {
            if (!isAcceptedChanges && !isInit) {
                ChangeInfo<Project> newProject = (ChangeInfo<Project>) event.getValue();
                List<Project> tempProjects = new ArrayList<>();
                tempProjects.add(newProject.getValue());
                bdiFeature.dispatchTopLevelGoal(new SendDataGoal(
                        new Socket("localhost", OTHER_AGENT_PORT),
                        tempProjects)
                ).get();
            }
            isAcceptedChanges = false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan(trigger = @Trigger(factaddeds = "projectsList"))
    public void sendNewProjectAdd(ChangeEvent event) {
        try {
            if (!isAcceptedChanges) {
                ChangeInfo<Project> newProject = (ChangeInfo<Project>) event.getValue();
                List<Project> tempProjects = new ArrayList<>();
                tempProjects.add(newProject.getValue());
                bdiFeature.dispatchTopLevelGoal(new SendDataGoal(
                        new Socket("localhost", OTHER_AGENT_PORT),
                        tempProjects)
                ).get();
            }
            isAcceptedChanges = false;
            isInit = false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Plan(trigger = @Trigger(goals = SendDataGoal.class))
    public void sendData(SendDataGoal goal) {
        try {
            Socket client = goal.getClient();
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            List<Project> data = goal.getData();
            out.writeObject(data);
            out.flush();
            System.out.println("Success!");
            client.close();
            out.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Goal
    public class GetRequiredExecutorsNumber {
        protected Project currentProject;
        protected int requiredExecutorsNumber;
        protected Duration expectedDuration;

        public GetRequiredExecutorsNumber(Project currentProject) {
            this.currentProject = currentProject;
        }

        public Project getCurrentProject() {
            return this.currentProject;
        }

        public int getRequiredExecutorsNumber() {
            return this.requiredExecutorsNumber;
        }

        public Duration getExpectedDuration() {
            return this.expectedDuration;
        }

        public void setRequiredExecutorsNumber(int requiredExecutorsNumber) {
            this.requiredExecutorsNumber = requiredExecutorsNumber;
        }

        public void setExpectedDuration(Duration expectedDuration) {
            this.expectedDuration = expectedDuration;
        }
    }

    @Goal
    public class AcceptDataGoal {
        protected Socket client;

        public AcceptDataGoal(Socket client) {
            this.client = client;
        }

        public Socket getClient() {
            return this.client;
        }

        public void setClient(Socket client) {
            this.client = client;
        }
    }

    @Goal
    public class SendDataGoal {
        protected Socket client;
        protected List<Project> data;

        public SendDataGoal(Socket client, List<Project> data) {
            this.client = client;
            this.data = data;
        }

        public Socket getClient() {
            return this.client;
        }

        public void setClient(Socket client) {
            this.client = client;
        }

        public List<Project> getData() {
            return this.data;
        }

        public void setData(List<Project> data) {
            this.data = data;
        }
    }
}