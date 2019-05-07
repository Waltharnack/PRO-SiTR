/*
 * Filename : Simulation.java
 * Creation date : 07.04.2019
 */

package ch.heigvd.sitr.model;

import ch.heigvd.sitr.gui.simulation.Displayer;
import ch.heigvd.sitr.gui.simulation.SimulationWindow;
import ch.heigvd.sitr.map.RoadNetwork;
import ch.heigvd.sitr.map.input.OpenDriveHandler;
import ch.heigvd.sitr.vehicle.Vehicle;
import ch.heigvd.sitr.vehicle.VehicleController;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.*;

/**
 * Simulation class handles all global simulation settings and values
 * The main simulation loop runs here as well
 *
 * @author Luc Wachter, Simon Walther
 */
public class Simulation {
    // The displayable component we need to repaint
    private Displayer window;
    // The scenario of the current simulation
    private ScenarioType scenario;
    // The behaviour the vehicles should have when arriving at their destination
    private VehicleBehaviourType behaviour;
    // List of vehicles generated by traffic generator
    private LinkedList<Vehicle> vehicles;

    // Rate at which the redrawing will happen in milliseconds
    private static final int UPDATE_RATE = 40;

    // Road network
    private final RoadNetwork roadNetwork;

    // The ratio px/m
    @Getter
    private double scale;

    // The timer for the main loop
    private Timer timer;

    @Getter
    private final double defaultDelta = 0.3;

    @Getter
    private double delta = defaultDelta;

    @Getter
    private double prevDelta = defaultDelta;

    // The task for the timer
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            for (Vehicle vehicle : vehicles) {
                vehicle.update(delta);
                vehicle.draw(scale);
                // DEBUG
                System.out.println(vehicle);
            }

            // TODO (tum) WTF we shouldn't do that
            // Print the road network
            roadNetwork.draw();

            // Callback to paintComponent()
            window.repaint();
        }
    };

    /**
     * Simulation constructor
     *
     * @param scenario    The scenario the simulation must create
     * @param behaviour   The behaviour the vehicles must adopt when arriving at their destination
     * @param controllers The number of vehicles for each controller type
     */
    public Simulation(ScenarioType scenario, VehicleBehaviourType behaviour,
                      HashMap<VehicleControllerType, Integer> controllers) {
        this.scenario = scenario;
        this.scale = scenario.getScale();
        this.behaviour = behaviour;

        // Generate vehicles from user parameters
        vehicles = generateTraffic(controllers);
    
        // Create a roadNetwork instance and then parse the OpenDRIVE XML file
        roadNetwork = new RoadNetwork();
        // TODO : Remove hard coded openDriveFilename
        parseOpenDriveXml(roadNetwork, "src/main/resources/map/simulation/simple_road.xodr");
    }

    /**
     * Main simulation loop, runs in a fixed rate timer loop
     */
    public void loop() {
        // Launch main window
        window = SimulationWindow.getInstance();

        // Create a timer to run the main loop
        timer = new Timer();

        // Schedule a task to run immediately, and then
        // every UPDATE_RATE per second
        timer.scheduleAtFixedRate(task, 0, UPDATE_RATE);
    }

    /**
     * Generate correct number of vehicle for each specified controller
     *
     * @param controllers The hash map containing the specified number of vehicles for each controller
     * @return a list of all vehicles in the simulation
     */
    private LinkedList<Vehicle> generateTraffic(HashMap<VehicleControllerType, Integer> controllers) {
        LinkedList<Vehicle> vehicles = new LinkedList<>();

        // TODO Manage positions and front vehicles

        // Hard coded, to test
        Vehicle wall = new Vehicle("regular.xml", new VehicleController(VehicleControllerType.AUTONOMOUS));
        wall.setPosition(100);

        // Iterate through the hash map
        for (Map.Entry<VehicleControllerType, Integer> entry : controllers.entrySet()) {
            // One controller for all vehicles of a given type
            VehicleController controller = new VehicleController(entry.getKey());

            // Generate as many vehicles as asked
            for (int i = 0; i < entry.getValue(); i++) {
                Vehicle v = new Vehicle("regular.xml", controller);
                v.setFrontVehicle(wall);
                vehicles.add(v);
            }
        }

        return vehicles;
    }

    /**
     * Convert meters per second to kilometers per hour
     *
     * @param mps The amount of m/s to convert
     * @return the corresponding amount of km/h
     */
    public static double mpsToKph(double mps) {
        // m/s => km/h : x * 3.6
        return mps * 3.6;
    }

    /**
     * Convert kilometers per hour to meters per second
     *
     * @param kph The amount of km/h to convert
     * @return the corresponding
     */
    public static double kphToMps(double kph) {
        // km/h => m/s : x / 3.6
        return kph / 3.6;
    }

    /**
     * Convert m to px
     *
     * @param scale the ratio px/m
     * @param m     the number of m
     * @return the number of px
     */
    public static int mToPx(double scale, double m) {
        return (int) Math.round(m * scale);
    }

    /**
     * Convert px to m
     *
     * @param scale the ratio px/m
     * @param px    the number of px
     * @return the number of px
     */
    public static double pxToM(double scale, int px) {
        return px / scale;
    }

    /**
     * Convert m to px
     *
     * @param m the number of m
     * @return the number of px
     */
    public int mToPx(double m) {
        return Simulation.mToPx(scale, m);
    }

    /**
     * Convert px to m
     *
     * @param px the number of px
     * @return the number of px
     */
    public double pxToM(int px) {
        return Simulation.pxToM(scale, px);
    }

    /**
     * Parse the OpenDrive XML file
     * @param roadNetwork The Road network that will contains OpenDrive road network
     * @param openDriveFilename The OpenDrive filename
     */
    public void parseOpenDriveXml(RoadNetwork roadNetwork, String openDriveFilename) {
        // TODO (TUM) Add some logs here
        File openDriveFile = new File(openDriveFilename);
        OpenDriveHandler.loadRoadNetwork(roadNetwork, openDriveFile);
    }

    /**
     * Method used to stop the timer. it is used when we close the current simulation
     */
    public void stopLoop() {
        timer.cancel();
    }

    /**
     * Method used to set the current delta. This method save the value before to change it
     * @param delta new value of delta
     */
    public void setDelta(double delta) {
        prevDelta = this.delta;
        this.delta = delta;
    }
}
