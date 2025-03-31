package pcd.ass01.Controller.Executor;

import pcd.ass01.Controller.ParallelController;
import pcd.ass01.Controller.SimulationStateHandler;
import pcd.ass01.View.BoidPattern.BoidPatterns;
import pcd.ass01.Model.BoidsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BoidsExecutor implements ParallelController, SimulationStateHandler {

    private ExecutorService boidRunners;
    private BoidsModel model;
    private int numberOfThreads;
    private BoidPatterns boidPatterns;

    private boolean isInitialized = false;

    public BoidsExecutor(BoidsModel model) {
        this.model = model;
        this.boidPatterns = new BoidPatterns();
        calculateNumberOfThreads();
    }

    public synchronized void start() {
        isInitialized = true;
        boidRunners = Executors.newFixedThreadPool(numberOfThreads);
    }

    public synchronized void update() {
        if (!isInitialized) {
            return;
        }
        if (model.isSuspended() || boidRunners.isShutdown()) {
            return;
        }

        this.updateVelocity();
        this.updatePosition();
        this.checkNumberOfBoidsValidity();
    }

    public synchronized void stop() {
        if (boidRunners.isShutdown()) {
            return;
        }
        boidRunners.shutdown();
        try {
            boidRunners.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("Error while shutting down the boid runners");
        }
        model.setBoids(0);
    }

    @Override
    public void resume() {
        model.resume();
    }

    @Override
    public void suspend() {
        model.suspend();
    }

    private void calculateNumberOfThreads() {
        numberOfThreads = detectNumberOfThreadsUsage();
    }

    private int detectNumberOfThreadsUsage() {
        final var numberOfAvailableProcessors = Runtime.getRuntime().availableProcessors() + 1;
        return Math.max(1, Math.min(numberOfAvailableProcessors, model.getBoids().size()));
    }

    private void checkNumberOfBoidsValidity() {
        if (model.getNumberOfBoids() != model.getBoids().size()) {
            model.setBoids(model.getNumberOfBoids());
        }
    }

    private void updateVelocity() {
        var futures = new ArrayList<Future<Void>>();
        model.getBoids().forEach((boid) -> {
            futures.add(boidRunners.submit(new UpdateBoidVelocityTask(boid, model)));
        });
        waitForFutures(futures);
    }

    private void updatePosition() {
        var futures = new ArrayList<Future<Void>>();
        model.getBoids().forEach((boid) -> {
            futures.add(boidRunners.submit(new UpdateBoidPositionTask(boid, model)));
        });
        waitForFutures(futures);
    }

    private void waitForFutures(List<Future<Void>> futures) {
        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}