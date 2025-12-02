package traffic;

import java.awt.Color;
import java.util.Random;

/**
 * Strategy for picking vehicle colors.
 */
public interface ColorStrategy {
    Color pickColor(Random random);
}
