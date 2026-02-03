package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

import java.awt.*;

public record NewWaypoint(GeoPosition position, Color color) implements Waypoint {
    @Override
    public GeoPosition getPosition() {
        return position;
    }
}

