package rasterizers;

import models.Line;
import rasters.Raster;

import java.awt.*;

public interface Rasterizer {

    void setColor(Color color);

    void setRaster(Raster raster);

    void rasterize(Line line);

    void rasterizeRectangle(int x1, int y1, int x2, int y2, int color, int thickness, int style, boolean correctionMode);

    void rasterizeCircle(int cx, int cy, int r, int color, int thickness, int style);

    void fillRectangle(int x1, int y1, int x2, int y2, int color);

    void fillCircle(int cx, int cy, int r, int color);

    void fillPolygon(java.util.List<models.Point> pts, int color);

}
