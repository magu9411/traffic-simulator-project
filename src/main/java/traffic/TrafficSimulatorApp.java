package traffic;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TrafficSimulatorApp extends JFrame {
    private final SimulationEngine engine = SimulationEngineBuilder.defaults().build();
    private final TrafficPanel canvas = new TrafficPanel(engine);
    private final JLabel throughputLabel = new JLabel("Flow: 0.00 vehicles/s");
    private final JLabel timeLabel = new JLabel("t = 0.0s");
    private JSlider spawnSlider;
    private JSlider speedSlider;
    private JCheckBox laneClosureBox;

    private long lastTickNanos = System.nanoTime();

    public TrafficSimulatorApp() {
        super("Traffic Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        engine.addObserver(snapshot -> {
            throughputLabel.setText(String.format("Flow: %.2f vehicles/s", snapshot.throughputPerSecond()));
            timeLabel.setText(String.format("t = %.1fs", snapshot.simTimeSeconds()));
            canvas.setSnapshot(snapshot);
            canvas.repaint();
        });
        engine.reset();
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel sliders = new JPanel();
        sliders.setLayout(new BoxLayout(sliders, BoxLayout.X_AXIS));

        spawnSlider = new JSlider(0, 120, 40);
        spawnSlider.setMajorTickSpacing(30);
        spawnSlider.setMinorTickSpacing(10);
        spawnSlider.setPaintLabels(true);
        spawnSlider.setPaintTicks(true);
        JLabel spawnLabel = new JLabel("Vehicles/min: 40");
        spawnSlider.addChangeListener(e -> {
            spawnLabel.setText("Vehicles/min: " + spawnSlider.getValue());
            engine.execute(new SimulationCommands.SetSpawnRateCommand(spawnSlider.getValue()));
        });

        speedSlider = new JSlider(10, 120, 70);
        speedSlider.setMajorTickSpacing(20);
        speedSlider.setMinorTickSpacing(10);
        speedSlider.setPaintLabels(true);
        speedSlider.setPaintTicks(true);
        JLabel speedLabel = new JLabel("Speed limit: 70 px/s");
        speedSlider.addChangeListener(e -> {
            speedLabel.setText("Speed limit: " + speedSlider.getValue() + " px/s");
            engine.execute(new SimulationCommands.SetSpeedLimitCommand(speedSlider.getValue()));
        });

        laneClosureBox = new JCheckBox("Close 1 lane on main road");
        laneClosureBox.addActionListener(e -> engine.execute(new SimulationCommands.ToggleLaneClosureCommand(laneClosureBox.isSelected())));

        JButton resetButton = new JButton("Reset traffic");
        resetButton.addActionListener(e -> resetSimulation());

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        throughputLabel.setPreferredSize(new Dimension(180, 20));
        statsPanel.add(throughputLabel);
        statsPanel.add(timeLabel);

        sliders.add(spawnLabel);
        sliders.add(Box.createHorizontalStrut(10));
        sliders.add(spawnSlider);
        sliders.add(Box.createHorizontalStrut(16));
        sliders.add(speedLabel);
        sliders.add(Box.createHorizontalStrut(10));
        sliders.add(speedSlider);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        bottomRow.add(laneClosureBox);
        bottomRow.add(resetButton);
        bottomRow.add(Box.createHorizontalStrut(12));
        bottomRow.add(statsPanel);

        panel.add(sliders, BorderLayout.NORTH);
        panel.add(bottomRow, BorderLayout.SOUTH);
        return panel;
    }

    private void resetSimulation() {
        engine.execute(new SimulationCommands.ResetCommand());
        engine.setLaneClosure(false);
        laneClosureBox.setSelected(false);
        spawnSlider.setValue(40);
        speedSlider.setValue(70);
        lastTickNanos = System.nanoTime();
    }

    private void startLoop() {
        Timer timer = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.nanoTime();
                double delta = (now - lastTickNanos) / 1_000_000_000.0;
                lastTickNanos = now;
                engine.update(delta);
            }
        });
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TrafficSimulatorApp app = new TrafficSimulatorApp();
            app.setVisible(true);
            app.startLoop();
        });
    }
}
