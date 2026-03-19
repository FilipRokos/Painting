package rasterizers;

import models.Line;
import models.Point;
import rasters.Raster;

import java.awt.*;

public class TrivialRasterizer implements Rasterizer {

    private Color defaultColor = Color.RED;
    private Raster raster;

    public TrivialRasterizer(Color color, Raster raster) {
        this.raster = raster;
        this.defaultColor = color;
    }

    @Override
    public void setColor(Color color) {
        defaultColor = color;
    }

    @Override
    public void setRaster(Raster raster) {
        this.raster = raster;
    }

    private static Point snap(Point p1, Point p2) {
        int x1 = p1.getX(), y1 = p1.getY();
        int x2 = p2.getX(), y2 = p2.getY();

        int dx = x2 - x1;
        int dy = y2 - y1;

        int adx = Math.abs(dx);
        int ady = Math.abs(dy);

        double ratio = 2.0;

        if (adx >= ratio * ady) {
            return new Point(x2, y1);
        }
        else if (ady >= ratio * adx) {
            return new Point(x1, y2);
        }
        else {
            int sx = Integer.compare(dx, 0);
            int sy = Integer.compare(dy, 0);
            int m = Math.max(adx, ady);
            return new Point(x1 + sx * m, y1 + sy * m);
        }
    }

    private void plotThick(int x, int y, int color, int thickness) {
        int r = Math.max(0, (Math.max(1, thickness) - 1) / 2);
        for (int yy = y - r; yy <= y + r; yy++) {
            for (int xx = x - r; xx <= x + r; xx++) {
                raster.setPixel(xx, yy, color);
            }
        }
    }

    private boolean styleDraw(int style, int stepIndex) {
        // 0 solid, 1 dashed, 2 dotted
        if (style == 0) return true;
        if (style == 2) {
            // draw one pixel every N steps
            return (stepIndex % 3) == 0;
        }
        // dashed
        int dash = 8;
        int gap = 5;
        int period = dash + gap;
        return (stepIndex % period) < dash;
    }

    @Override
    public void rasterize(Line line) {

        // keep legacy behavior but with color/thickness/style
        int color = line.getColor();
        int thickness = line.getThickness();
        int style = line.getStyle();

        Point p1 = line.getP1();
        Point p2 = line.getP2();
        if (line.isCorrectionMode()) {
            p2 = snap(p1, p2);
            line.setP2(p2);
        }
        rasterizeRectangleLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), color, thickness, style);
    }

    private void rasterizeRectangleLine(int x1, int y1, int x2, int y2, int color, int thickness, int style) {
        // vertical
        if (x1 == x2) {
            if (y1 > y2) { int t = y1; y1 = y2; y2 = t; }
            int i = 0;
            for (int y = y1; y <= y2; y++) {
                if (styleDraw(style, i)) plotThick(x1, y, color, thickness);
                i++;
            }
            return;
        }

        double k = (double) (y2 - y1) / (x2 - x1);
        double q = y1 - k * x1;

        if (Math.abs(k) < 1) {
            if (x1 > x2) {
                int tx = x1; x1 = x2; x2 = tx;
                int ty = y1; y1 = y2; y2 = ty;
                k = (double) (y2 - y1) / (x2 - x1);
                q = y1 - k * x1;
            }
            int i = 0;
            for (int x = x1; x <= x2; x++) {
                int y = (int) Math.round(k * x + q);
                if (styleDraw(style, i)) plotThick(x, y, color, thickness);
                i++;
            }
        } else {
            if (y1 > y2) {
                int tx = x1; x1 = x2; x2 = tx;
                int ty = y1; y1 = y2; y2 = ty;
                k = (double) (y2 - y1) / (x2 - x1);
                q = y1 - k * x1;
            }
            int i = 0;
            for (int y = y1; y <= y2; y++) {
                int x = (int) Math.round((y - q) / k);
                if (styleDraw(style, i)) plotThick(x, y, color, thickness);
                i++;
            }
        }
    }

    @Override
    public void rasterizeRectangle(int x1, int y1, int x2, int y2, int color, int thickness, int style, boolean correctionMode) {
        // draw 4 edges; if correctionMode = true, snap the diagonal (so rectangle behaves like aligned with shift)
        if (correctionMode) {
            Point p1 = new Point(x1, y1);
            Point p2 = snap(p1, new Point(x2, y2));
            x2 = p2.getX();
            y2 = p2.getY();
        }
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);

        rasterizeRectangleLine(left, top, right, top, color, thickness, style);
        rasterizeRectangleLine(right, top, right, bottom, color, thickness, style);
        rasterizeRectangleLine(right, bottom, left, bottom, color, thickness, style);
        rasterizeRectangleLine(left, bottom, left, top, color, thickness, style);
    }

    @Override
    public void rasterizeCircle(int cx, int cy, int r, int color, int thickness, int style) {
        r = Math.max(0, r);
        int x = 0;
        int y = r;
        int d = 1 - r;

        int stepIndex = 0;
        while (x <= y) {
            if (styleDraw(style, stepIndex)) {
                plotThick(cx + x, cy + y, color, thickness);
                plotThick(cx + y, cy + x, color, thickness);
                plotThick(cx - x, cy + y, color, thickness);
                plotThick(cx - y, cy + x, color, thickness);
                plotThick(cx + x, cy - y, color, thickness);
                plotThick(cx + y, cy - x, color, thickness);
                plotThick(cx - x, cy - y, color, thickness);
                plotThick(cx - y, cy - x, color, thickness);
            }
            stepIndex++;

            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }
    }

    @Override
    public void fillRectangle(int x1, int y1, int x2, int y2, int color) {
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                raster.setPixel(x, y, color);
            }
        }
    }

    @Override
    public void fillCircle(int cx, int cy, int r, int color) {
        r = Math.max(0, r);
        for (int y = -r; y <= r; y++) {
            int xx = (int) Math.floor(Math.sqrt(r * (double) r - y * (double) y));
            for (int x = -xx; x <= xx; x++) {
                raster.setPixel(cx + x, cy + y, color);
            }
        }
    }

    @Override
    public void fillPolygon(java.util.List<Point> pts, int color) {
        if (pts == null || pts.size() < 3) return;

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Point p : pts) {
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        java.util.List<Integer> nodes = new java.util.ArrayList<>();

        for (int y = minY; y <= maxY; y++) {
            nodes.clear();
            int j = pts.size() - 1;
            for (int i = 0; i < pts.size(); i++) {
                int yi = pts.get(i).getY();
                int yj = pts.get(j).getY();
                int xi = pts.get(i).getX();
                int xj = pts.get(j).getX();
                boolean cond = (yi < y && yj >= y) || (yj < y && yi >= y);
                if (cond) {
                    int x = xi + (int) Math.round((y - yi) * (xj - xi) / (double) (yj - yi));
                    nodes.add(x);
                }
                j = i;
            }
            nodes.sort(Integer::compareTo);
            for (int k2 = 0; k2 + 1 < nodes.size(); k2 += 2) {
                int xStart = nodes.get(k2);
                int xEnd = nodes.get(k2 + 1);
                for (int x = xStart; x <= xEnd; x++) {
                    raster.setPixel(x, y, color);
                }
            }
        }
    }
}
