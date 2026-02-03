package org.example;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.viewer.*;
import org.jxmapviewer.painter.Painter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class GUI extends JFrame {
    private final JXMapViewer mapViewer = new JXMapViewer();
    private List<NewWaypoint> wasteSiteWaypoints;
    private List<NewWaypoint> clusterWaypoints;
    private final Random rand = new Random();

    public GUI(int width, int height) {
        setTitle("Waste Site Clustering");
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeMap();
        setVisible(true);
    }

    private void initializeMap() {
        setLayout(new BorderLayout());

        OSMTileFactoryInfo info = new OSMTileFactoryInfo(
                "Carto",
                "https://a.basemaps.cartocdn.com/light_all"
        );

        DefaultTileFactory factory = new DefaultTileFactory(info);
        factory.setThreadPoolSize(4);
        factory.setUserAgent("Map/1.0");

        mapViewer.setTileFactory(factory);

        mapViewer.setAddressLocation(new GeoPosition(48.5, 10));
        mapViewer.setZoom(13);

        mapViewer.addMouseMotionListener(new PanMouseInputListener(mapViewer));
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));
        mapViewer.addMouseListener(new PanMouseInputListener(mapViewer));

        add(mapViewer, BorderLayout.CENTER);
    }

    public void drawClusters(List<Cluster> clusters) {
        wasteSiteWaypoints = new ArrayList<>();
        clusterWaypoints = new ArrayList<>();

        for (Cluster cluster : clusters) {
            Color clusterColor = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));

            for (WasteSite wasteSite : cluster.getWasteSiteList()) {
                GeoPosition wasteSitePosition = new GeoPosition(wasteSite.la(), wasteSite.lo());
                wasteSiteWaypoints.add(new NewWaypoint(wasteSitePosition, clusterColor));
            }

            GeoPosition clusterPosition = new GeoPosition(cluster.getLa(), cluster.getLo());
            clusterWaypoints.add(new NewWaypoint(clusterPosition, Color.BLACK));
        }


        Painter<JXMapViewer> overlayPainter = (g, map, width, height) -> {
            if (wasteSiteWaypoints == null || clusterWaypoints == null) {
                return;
            }

            for (NewWaypoint waypoint : wasteSiteWaypoints) {
                drawWaypoint(g, map, waypoint, waypoint.color(), 4);
            }

            for (NewWaypoint waypoint : clusterWaypoints) {
                drawWaypoint(g, map, waypoint, Color.BLACK, 8);
            }
        };

        mapViewer.setOverlayPainter(overlayPainter);
        mapViewer.repaint();
    }

    private void drawWaypoint(Graphics2D g, JXMapViewer map, NewWaypoint waypoint,
                              Color color, int radius) {

        Point2D point = map.convertGeoPositionToPoint(waypoint.position());
        int x = (int) point.getX();
        int y = (int) point.getY();

        g.setColor(color);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setColor(Color.BLACK);
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
    }
}