package ui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Renderer extends Thread {
    private final Queue<String> renderQueue = new ConcurrentLinkedQueue<>();

    public void enqueueRenderTask(String task) {
        renderQueue.offer(task);
    }

    public void enqueueRenderTasks(String[] tasks) {
        for (String task : tasks) {
            renderQueue.offer(task);
        }
    }

    public void run() {
        while (GameManager.running) {
            try {
                if (renderQueue.isEmpty()) {
                    Thread.sleep(64);
                } else {
                    String task = renderQueue.poll();

                    if (task != null) {
                        System.out.println(task);
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
