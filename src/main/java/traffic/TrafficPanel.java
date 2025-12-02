package traffic;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.List;

public class TrafficPanel extends JPanel {
    private final SimulationEngine engine;
    private SimulationSnapshot snapshot;
    private final Color asphalt = new Color(0x2b2b2b);
    private final Color laneMarking = new Color(0xf1c40f);
    private final Color closureColor = new Color(0xc0392b);

    public TrafficPanel(SimulationEngine engine) {
        this.engine = engine;
        this.snapshot = engine.createSnapshot();
        setPreferredSize(new Dimension(820, 500));
        setBackground(new Color(0x121212));
        setDoubleBuffered(true);
    }

    public void setSnapshot(SimulationSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (snapshot == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawRoads(g2);
        drawSignals(g2);
        drawVehicles(g2);
        g2.dispose();
    }

    private void drawRoads(Graphics2D g2) {
        List<SimulationEngine.RoadView> roads = snapshot.roadViews();
        for (SimulationEngine.RoadView road : roads) {
            float laneWidth = 12f;
            float totalWidth = (float) (road.totalLanes() * laneWidth + 6);
            BasicStroke baseStroke = new BasicStroke(totalWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2.setStroke(baseStroke);
            g2.setColor(asphalt);
            g2.drawLine((int) road.start().x, (int) road.start().y, (int) road.end().x, (int) road.end().y);

            if (road.openLanes() < road.totalLanes()) {
                float closedWidth = (road.totalLanes() - road.openLanes()) * laneWidth;
                if (closedWidth > 0) {
                    BasicStroke closedStroke = new BasicStroke(closedWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                    g2.setStroke(closedStroke);
                    g2.setColor(closureColor);
                    g2.drawLine((int) road.start().x, (int) road.start().y, (int) road.end().x, (int) road.end().y);
                }
            }

            if (road.totalLanes() > 1) {
                float markingWidth = 2f;
                BasicStroke markingStroke = new BasicStroke(markingWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8f, 8f}, 0);
                g2.setStroke(markingStroke);
                g2.setColor(laneMarking);
                g2.drawLine((int) road.start().x, (int) road.start().y, (int) road.end().x, (int) road.end().y);
            }

            g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
            g2.setColor(new Color(0xcccccc));
            Point2D.Double labelPos = midpoint(road.start(), road.end());
            g2.drawString(road.name(), (int) labelPos.x + 8, (int) labelPos.y - 8);
        }
    }

    private void drawSignals(Graphics2D g2) {
        SimulationEngine.SignalView signal = snapshot.signalView();
        int size = 12;
        // Main road light drawn above intersection; cross-road light drawn to the right.
        int mainX = (int) signal.position().x - size / 2;
        int mainY = (int) signal.position().y - 22;
        int crossX = (int) signal.position().x + 12;
        int crossY = (int) signal.position().y - size / 2;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(mainX - 2, mainY - 2, size + 4, size + 4, 6, 6);
        g2.fillRoundRect(crossX - 2, crossY - 2, size + 4, size + 4, 6, 6);

        g2.setColor(signal.mainColor());
        g2.fillOval(mainX, mainY, size, size);
        g2.setColor(signal.crossColor());
        g2.fillOval(crossX, crossY, size, size);
    }

    private void drawVehicles(Graphics2D g2) {
        for (SimulationEngine.VehicleView vehicle : snapshot.vehicleViews()) {
            int size = vehicle.fromRamp() ? 10 : 12;
            int x = (int) vehicle.position().x - size / 2;
            int y = (int) vehicle.position().y - size / 2;
            g2.setColor(vehicle.color());
            g2.fillRoundRect(x, y, size, size, 6, 6);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(x, y, size, size, 6, 6);
        }
    }

    private Point2D.Double midpoint(Point2D.Double a, Point2D.Double b) {
        return new Point2D.Double((a.x + b.x) / 2.0, (a.y + b.y) / 2.0);
    }
}
