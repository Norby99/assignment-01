package pcd.ass01.Controller.DefaultParallelism;

import java.util.ArrayList;
import java.util.List;

import pcd.ass01.BoidsSimulation;
import pcd.ass01.Controller.ParallelController;
import pcd.ass01.Controller.SimulationStateHandler;
import pcd.ass01.Model.Boid;
import pcd.ass01.Model.BoidsModel;
import pcd.ass01.View.BoidPattern.BoidPatterns;

public class PlatformThreadBoids implements ParallelController, SimulationStateHandler {
    private List<BoidRunner> boidRunners;
    private BoidsMonitor barrier;
    private BoidsModel model;
    private int numberOfThreads;
    private BoidPatterns boidPatterns = new BoidPatterns();

    public PlatformThreadBoids(BoidsModel model) {
        this.model = model;
        boidRunners = new ArrayList<>();
        createAndAssignBoidRunners();
    }

    public synchronized void start() {
        model.start();
        boidRunners.forEach(boidRunner -> Thread.ofPlatform().start(boidRunner));
    }

    public synchronized void update() {
        if (model.isSuspended() || !model.isRunning()) {
            return;
        }
        this.updateVelocity();
        this.updatePosition();
        this.checkThreadValidity();
    }

    public synchronized void stop() {
        model.setBoids(0);
        deleteThreads();
        model.stop();
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
        numberOfThreads = 8;
    }

    private void createAndAssignBoidRunners() {
        calculateNumberOfThreads();
        this.barrier = new BoidsMonitor(numberOfThreads + 1);
        final var boids = model.getBoids();
        var chunkSize = Math.max(1, boids.size() / numberOfThreads);
        var boidsGroupedInChunks = getBoidsGroupedInChunks(boids, numberOfThreads, chunkSize);

        // assigning patterns to each BoidRunner
        this.boidPatterns.resetPatterns();
        boidsGroupedInChunks.forEach((boidChunk) -> {
            BoidPatterns.Pattern assignedPattern = BoidsSimulation.DEFAULT_PATTERN;
            boidRunners.add(new BoidRunner(boidChunk, model, barrier, assignedPattern));
        });
    }

    private void checkThreadValidity() {
        try {
            if (model.getNumberOfBoids() != model.getBoids().size()) {
                redistributeBoids();
            }
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteThreads() {
        boidRunners.stream().forEach(BoidRunner::stop);
        boidRunners.clear();
    }

    private synchronized void redistributeBoids() {
        deleteThreads();
        model.setBoids(model.getNumberOfBoids());
        createAndAssignBoidRunners();
        start();
    }

    private ArrayList<List<Boid>> getBoidsGroupedInChunks(final List<Boid> boids, final int numberOfThreads,
            int chunkSize) {
        var boidsGroupedInChunks = new ArrayList<List<Boid>>();
        for (int i = 0; i < numberOfThreads; i++) {
            var start = i * chunkSize;
            var end = Math.min((i + 1) * chunkSize, boids.size());
            if (i == numberOfThreads - 1) {
                end = boids.size();
            }
            boidsGroupedInChunks.add(boids.subList(start, end));
        }
        return boidsGroupedInChunks;
    }

    private void updateVelocity() {
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePosition() {
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
