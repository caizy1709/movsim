package org.movsim.viewer.javafx;

import javafx.scene.canvas.GraphicsContext;
import org.jfree.fx.FXGraphics2D;
import org.movsim.roadmappings.RoadMapping;
import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.roadnetwork.controller.TrafficLight;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.utilities.Colors;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.Iterator;

public class DrawMovables {
    private ViewerSettings settings;
    private RoadNetwork roadNetwork;
    private final SimulationRunnable simulationRunnable;

    // pre-allocate clipping path for road mappings
    private final GeneralPath clipPath = new GeneralPath(Path2D.WIND_EVEN_ODD);

    private final double[] accelerations = new double[]{-7.5, -0.1, 0.2};
    private javafx.scene.paint.Color[] accelerationColors;
    private long vehicleToHighlightId = -1;


    public DrawMovables(ViewerSettings settings, RoadNetwork roadNetwork, SimulationRunnable simulationRunnable) {
        this.settings = settings;
        this.roadNetwork = roadNetwork;
        this.simulationRunnable = simulationRunnable;

    }


    /**
     * <p>
     * Draws the foreground: everything that moves each timestep. For the traffic simulation that means draw all the vehicles and traffic lights:<br />
     * For each roadSection, draw all the vehicles in the roadSection, positioning them using the roadMapping for that roadSection.
     * </p>
     *
     * <p>
     * This method is synchronized with the <code>SimulationRunnable.run()</code> method, so that vehicles are not updated, added or removed
     * while they are being drawn.
     * </p>
     * <p>
     */
    public void update(FXGraphics2D fxGraphics2D, GraphicsContext gc) {
        drawMovables(fxGraphics2D, gc);
    }

    private void drawMovables(FXGraphics2D g, GraphicsContext gc) {
        // moveVehicles occurs in the UI thread, so must synchronize with the
        // update of the road network in the calculation thread.
        final long timeBeforePaint_ms = System.currentTimeMillis();
        synchronized (simulationRunnable.dataLock) {
            drawTrafficLights(g);
            final double simulationTime = simulationRunnable.simulationTime();
            for (final RoadSegment roadSegment : roadNetwork) {
                final RoadMapping roadMapping = roadSegment.roadMapping();
                assert roadMapping != null;
                PaintRoadMappingFx.setClipPath(g, roadMapping, clipPath);
                for (final Vehicle vehicle : roadSegment) {
                    drawVehicle(gc, simulationTime, roadMapping, vehicle);
                }
                for (Iterator<Vehicle> vehIter = roadSegment.overtakingVehicles(); vehIter.hasNext(); ) {
                    Vehicle vehicle = vehIter.next();
                    drawVehicle(gc, simulationTime, roadMapping, vehicle);
                }
            }
//            totalAnimationTime += System.currentTimeMillis() - timeBeforePaint_ms;
        }
    }

    private void drawTrafficLights(FXGraphics2D g) {
        int strokeWidth = 3;
        for (final RoadSegment roadSegment : roadNetwork) {
            assert roadSegment.trafficLights() != null;
            for (TrafficLight trafficLight : roadSegment.trafficLights()) {
                java.awt.Color color = getTrafficLightColor(trafficLight);
                PaintRoadMappingFx.drawLine(g, roadSegment.roadMapping(), trafficLight.position(), strokeWidth, color);
            }
        }
    }

    private void drawVehicle(GraphicsContext gc, double simulationTime, RoadMapping roadMapping, Vehicle vehicle) {
        // draw vehicle polygon at new position
        final RoadMapping.PolygonFloat polygon = roadMapping.mapFloat(vehicle);

        gc.setFill(vehicleColor(vehicle, simulationTime));
        gc.beginPath();
        gc.moveTo(polygon.getXPoint(0), polygon.getYPoint(0));
        gc.lineTo(polygon.getXPoint(1), polygon.getYPoint(1));
        gc.lineTo(polygon.getXPoint(2), polygon.getYPoint(2));
        gc.lineTo(polygon.getXPoint(3), polygon.getYPoint(3));
        gc.closePath();
        gc.fill();

        if (vehicle.isBrakeLightOn()) {
            // if the vehicle is decelerating then display the
            gc.setStroke(javafx.scene.paint.Color.RED);
            gc.setLineWidth(4);
            gc.beginPath();
            // points 2 & 3 are at the rear of vehicle
            if (roadMapping.isPeer()) {
                gc.moveTo(polygon.getXPoint(0), polygon.getYPoint(0));
                gc.lineTo(polygon.getXPoint(1), polygon.getYPoint(1));
            } else {
                gc.moveTo(polygon.getXPoint(2), polygon.getYPoint(2));
                gc.lineTo(polygon.getXPoint(3), polygon.getYPoint(3));
            }
            gc.closePath();
            gc.stroke();
        }
    }

    private javafx.scene.paint.Color awtColorToJavaFX(Color c) {
        return javafx.scene.paint.Color.rgb(c.getRed(), c.getGreen(),
                c.getBlue(), c.getAlpha() / 255.0);
    }

    private java.awt.Color getTrafficLightColor(TrafficLight trafficLight) {
        java.awt.Color color = null;
        switch (trafficLight.status()) {
            case GREEN:
                color = java.awt.Color.GREEN;
                break;
            case GREEN_RED:
                color = java.awt.Color.YELLOW;
                break;
            case RED:
                color = java.awt.Color.RED;
                break;
            case RED_GREEN:
                color = java.awt.Color.ORANGE;
                break;
        }
        return color;
    }


    protected javafx.scene.paint.Color vehicleColor(Vehicle vehicle, double simulationTime) {
        javafx.scene.paint.Color color;

        switch (settings.getVehicleColorMode()) {
            case ACCELERATION_COLOR:
                final double a = vehicle.physicalQuantities().getAcc();
                final int count = accelerations.length;
                for (int i = 0; i < count; ++i) {
                    if (a < accelerations[i])
                        return accelerationColors[i];
                }
                color = accelerationColors[accelerationColors.length - 1];
                break;
            case EXIT_COLOR:
                color = javafx.scene.paint.Color.BLACK;
                if (vehicle.exitRoadSegmentId() != Vehicle.ROAD_SEGMENT_ID_NOT_SET) {
                    color = javafx.scene.paint.Color.WHITE;
                }
                break;
            case HIGHLIGHT_VEHICLE:
                color = vehicle.getId() == vehicleToHighlightId ? javafx.scene.paint.Color.BLUE : javafx.scene.paint.Color.BLACK;
                break;
            case LANE_CHANGE:
                color = javafx.scene.paint.Color.BLACK;
                if (vehicle.inProcessOfLaneChange()) {
                    color = javafx.scene.paint.Color.ORANGE;
                }
                break;
            case VEHICLE_COLOR:
                // use vehicle's cache for AWT color object
                color = (javafx.scene.paint.Color) vehicle.colorObject();
                if (color == null) {
                    int vehColorInt = vehicle.color();
                    color = javafx.scene.paint.Color.rgb(vehColorInt, vehColorInt, vehColorInt);
                    vehicle.setColorObject(color);
                }
                break;
            case VEHICLE_LABEL_COLOR:
                String label = vehicle.getLabel();
                color = settings.getLabelColors().containsKey(label) ? settings.getLabelColors().get(label) : javafx.scene.paint.Color.WHITE;
                break;
            case VELOCITY_COLOR:
                double v = vehicle.physicalQuantities().getSpeed() * 3.6;
                color = getColorAccordingToSpectrum2(0, settings.getVmaxForColorSpectrum(), v);
                break;
            default:
                throw new IllegalStateException("unknown vehicleColorMode" + settings.getVehicleColorMode());
        }
        return color;
    }

    /**
     * hue values (see, e.g., http://help.adobe.com/en_US/Photoshop/11.0/images/wc_HSB.png): h=0:red, h=0.2: yellow, h=0.35: green, h=0.5:
     * blue, h=0.65: violet, then a long magenta region
     **/
    private javafx.scene.paint.Color getColorAccordingToSpectrum2(double vmin, double vmax, double v) {
        assert vmax > vmin;
        // tune following values if not satisfied
        // (the floor function of any hue value >=1 will be subtracted by HSBtoRGB)

        final double hue_vmin = 1.00; // hue value for minimum speed value; red
        final double hue_vmax = 1.84; // hue value for max speed (1 will be subtracted); violetblue

        // possibly a nonlinear hue(speed) function looks nicer;
        // first try this truncuated-linear one

        float vRelative = (vmax > vmin) ? (float) ((v - vmin) / (vmax - vmin)) : 0;
        vRelative = Math.min(Math.max(0, vRelative), 1);
        final float h = (float) (hue_vmin + vRelative * (hue_vmax - hue_vmin));

        // use max. saturation
        final float s = (float) 1.0;

        // possibly a reduction of brightness near h=0.5 looks nicer;
        // first try max brightness (0-1)
        final float b = (float) 0.92;

        return v > 0.1 ? javafx.scene.paint.Color.hsb(((h -1) * 360), s, b) : javafx.scene.paint.Color.BLACK;
    }

    void setAccelerationColors() {
        accelerationColors = new javafx.scene.paint.Color[]{javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.RED, javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GREEN};
        assert accelerations.length == accelerationColors.length - 1;
    }

}
