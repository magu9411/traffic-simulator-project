package traffic;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getPoint());
            }
        });
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
        LightPositions positions = computeLightPositions(signal);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(positions.mainBounds.x - 2, positions.mainBounds.y - 2, positions.size + 4, positions.size + 4, 6, 6);
        g2.fillRoundRect(positions.crossBounds.x - 2, positions.crossBounds.y - 2, positions.size + 4, positions.size + 4, 6, 6);

        g2.setColor(signal.mainColor());
        g2.fillOval(positions.mainBounds.x, positions.mainBounds.y, positions.size, positions.size);
        g2.setColor(signal.crossColor());
        g2.fillOval(positions.crossBounds.x, positions.crossBounds.y, positions.size, positions.size);
    }

    private void drawVehicles(Graphics2D g2) {
        for (SimulationEngine.VehicleView vehicle : snapshot.vehicleViews()) {
            int size = 12;
            int arc = size/2;
            int x = (int) vehicle.position().x - size / 2;
            int y = (int) vehicle.position().y - size / 2;

            int border = 2;
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(x - border, y - border, size + border * 2, size + border * 2, arc + border, arc + border);
            g2.setColor(vehicle.color());
            g2.fillRoundRect(x, y, size, size, arc, arc);
        }
    }

    private Point2D.Double midpoint(Point2D.Double a, Point2D.Double b) {
        return new Point2D.Double((a.x + b.x) / 2.0, (a.y + b.y) / 2.0);
    }

    private void handleClick(Point point) {
        if (snapshot == null) {
            return;
        }
        SimulationEngine.SignalView signal = snapshot.signalView();
        LightPositions positions = computeLightPositions(signal);
        if (positions.mainBounds.contains(point)) {
            engine.execute(new SimulationCommands.SetPhaseCommand(SimulationEngine.Intersection.Phase.MAIN_GREEN));
        } else if (positions.crossBounds.contains(point)) {
            engine.execute(new SimulationCommands.SetPhaseCommand(SimulationEngine.Intersection.Phase.CROSS_GREEN));
        }
    }

    private LightPositions computeLightPositions(SimulationEngine.SignalView signal) {
        int size = 16;
        int offset = 40;
        int mainCenterX = (int) Math.round(signal.position().x + offset);
        int mainCenterY = (int) Math.round(signal.position().y);
        int crossCenterX = (int) Math.round(signal.position().x);
        int crossCenterY = (int) Math.round(signal.position().y - offset);
        Rectangle mainBounds = new Rectangle(mainCenterX - size / 2, mainCenterY - size / 2, size, size);
        Rectangle crossBounds = new Rectangle(crossCenterX - size / 2, crossCenterY - size / 2, size, size);
        return new LightPositions(mainBounds, crossBounds, size);
    }

    private record LightPositions(Rectangle mainBounds, Rectangle crossBounds, int size) {
    }
}
