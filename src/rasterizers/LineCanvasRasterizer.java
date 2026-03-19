package rasterizers;

import models.Line;
import models.LineCanvas;
import models.Point;

import java.util.List;

public class LineCanvasRasterizer {

    private final Rasterizer rasterizer;

    public LineCanvasRasterizer(Rasterizer rasterizer) {
        this.rasterizer = rasterizer;
    }

    public void rasterizeCanvas(LineCanvas lineCanvas) {
        if (lineCanvas == null) return;

        for (int i = 0; i < lineCanvas.size(); i++) {
            int type = lineCanvas.getType(i);
            int stroke = lineCanvas.getStrokeColor(i);
            int thick = lineCanvas.getThickness(i);
            int style = lineCanvas.getStyle(i);

            boolean filled = lineCanvas.isFilled(i);
            int fill = lineCanvas.getFillColor(i);

            Object g = lineCanvas.getGeometry(i);

            if (type == LineCanvas.OBJ_LINE) {
                rasterizer.rasterize((Line) g);
            }
            else if (type == LineCanvas.OBJ_RECT || type == LineCanvas.OBJ_SQUARE) {
                int[] a = (int[]) g;
                int x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
                if (type == LineCanvas.OBJ_SQUARE) {

                    int dx = x2 - x1;
                    int dy = y2 - y1;
                    int s = Math.max(Math.abs(dx), Math.abs(dy));
                    x2 = x1 + Integer.signum(dx) * s;
                    y2 = y1 + Integer.signum(dy) * s;
                }

                if (filled) rasterizer.fillRectangle(x1, y1, x2, y2, fill);
                rasterizer.rasterizeRectangle(x1, y1, x2, y2, stroke, thick, style, false);
            }
            else if (type == LineCanvas.OBJ_CIRCLE) {
                int[] a = (int[]) g;
                int cx = a[0], cy = a[1], r = a[2];
                if (filled) rasterizer.fillCircle(cx, cy, r, fill);
                rasterizer.rasterizeCircle(cx, cy, r, stroke, thick, style);
            }
            else if (type == LineCanvas.OBJ_POLYGON) {
                List<Point> pts = (List<Point>) g;
                if (filled) rasterizer.fillPolygon(pts, fill);
                if (pts.size() >= 2) {
                    for (int p = 0; p < pts.size(); p++) {
                        Point a = pts.get(p);
                        Point b = pts.get((p + 1) % pts.size());
                        Line l = new Line(a, b);
                        l.setColor(stroke);
                        l.setThickness(thick);
                        l.setStyle(style);
                        rasterizer.rasterize(l);
                    }
                }
            }
        }
    }
}
