package traffic;

import java.awt.Color;
import java.util.Random;

/**
 * Palette-based color selection strategy.
 */
public class PaletteColorStrategy implements ColorStrategy {
    private final Color[] palette = {
            new Color(0x2E86DE),
            new Color(0x27AE60),
            new Color(0xF39C12),
            new Color(0xE74C3C),
            new Color(0x8E44AD)
    };

    @Override
    public Color pickColor(Random random) {
        return palette[random.nextInt(palette.length)];
    }
}
