package ui;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class Renderer extends Thread {
    private final Queue<String> renderQueue = new LinkedList<>();

    public void enqueueRenderTask(String task) {
        synchronized (renderQueue) {
            renderQueue.add(task);
            renderQueue.notify();
        }
    }

    public void run() {
        while (GameManager.running) {
            // Rendering logic goes here
            try {
                Thread.sleep(64); // Approx. 15 FPS

                if (renderQueue.isEmpty()) {
                    synchronized (renderQueue) {
                        renderQueue.wait();
                    }
                } else {
                    String task;
                    synchronized (renderQueue) {
                        task = renderQueue.poll();
                    }
                    if (task != null) {
                        System.out.println("Rendering task: " + task);
                    }
                }

            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
