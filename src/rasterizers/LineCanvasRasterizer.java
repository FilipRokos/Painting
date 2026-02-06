package rasterizers;

import models.Line;
import models.LineCanvas;

public class LineCanvasRasterizer {

    private final Rasterizer rasterizer;

    public LineCanvasRasterizer(Rasterizer rasterizer) {
        this.rasterizer = rasterizer;
    }

    public void rasterizeCanvas(LineCanvas lineCanvas) {
        if (lineCanvas == null) return;

        for (Line line : lineCanvas.getLines()) {
            if (line == null) continue;
            rasterizer.rasterize(line);
        }
    }
}
