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

        if (adx >= ratio * ady) { //horizontála
            return new Point(x2, y1);
        }
        else if (ady >= ratio * adx) { //vertikála
            return new Point(x1, y2);
        }
        else { //diagonála
            int sx = Integer.compare(dx, 0);
            int sy = Integer.compare(dy, 0);
            int m = Math.max(adx, ady);
            return new Point(x1 + sx * m, y1 + sy * m);
        }
    }

    @Override
    public void rasterize(Line line) {

        // 1) Correction mode: uprav P2 ještě před výpočty
        if (line.isCorrectionMode()) {
            line.setP2(snap(line.getP1(), line.getP2()));
        }

        int x1 = line.getP1().getX();
        int y1 = line.getP1().getY();
        int x2 = line.getP2().getX();
        int y2 = line.getP2().getY();


        if (x1 == x2) {

            if (y1 > y2) {
                int t = y1; y1 = y2; y2 = t;
            }

            int step = line.isDotted() ? 3 : 1;

            for (int y = y1; y <= y2; y += step) {
                raster.setPixel(x1, y, defaultColor.getRGB());
            }
            return;
        }

        // 3) Standardní případ: použij k/q
        double k = (double) (y2 - y1) / (x2 - x1);
        double q = y1 - k * x1;

        if (Math.abs(k) < 1) {

            // seřadit podle X
            if (x1 > x2) {
                int tx = x1; x1 = x2; x2 = tx;
                int ty = y1; y1 = y2; y2 = ty;

                // přepočítat k/q po prohození (bezpečné)
                k = (double) (y2 - y1) / (x2 - x1);
                q = y1 - k * x1;
            }

            int step = line.isDotted() ? 3 : 1;

            for (int x = x1; x <= x2; x += step) {
                int y = (int) Math.round(k * x + q);
                raster.setPixel(x, y, defaultColor.getRGB());
            }

        } else {

            // seřadit podle Y
            if (y1 > y2) {
                int tx = x1; x1 = x2; x2 = tx;
                int ty = y1; y1 = y2; y2 = ty;

                k = (double) (y2 - y1) / (x2 - x1);
                q = y1 - k * x1;
            }

            int step = line.isDotted() ? 3 : 1;

            for (int y = y1; y <= y2; y += step) {
                int x = (int) Math.round((y - q) / k);
                raster.setPixel(x, y, defaultColor.getRGB());
            }
        }
    }
}
