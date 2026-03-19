package models;

import java.util.ArrayList;
import java.util.List;


public class LineCanvas {

    public static final int OBJ_LINE = 0;
    public static final int OBJ_RECT = 1;
    public static final int OBJ_SQUARE = 2;
    public static final int OBJ_CIRCLE = 3;
    public static final int OBJ_POLYGON = 4;

    private final List<Integer> types = new ArrayList<>();
    private final List<Object> geometry = new ArrayList<>();

    private final List<Integer> strokeColor = new ArrayList<>();
    private final List<Integer> thickness = new ArrayList<>();
    private final List<Integer> style = new ArrayList<>();

    private final List<Boolean> filled = new ArrayList<>();
    private final List<Integer> fillColor = new ArrayList<>();

    public int size() {
        return types.size();
    }

    public int getType(int idx) {
        return types.get(idx);
    }

    public Object getGeometry(int idx) {
        return geometry.get(idx);
    }

    public int getStrokeColor(int idx) {
        return strokeColor.get(idx);
    }

    public int getThickness(int idx) {
        return thickness.get(idx);
    }

    public int getStyle(int idx) {
        return style.get(idx);
    }

    public boolean isFilled(int idx) {
        return filled.get(idx);
    }

    public int getFillColor(int idx) {
        return fillColor.get(idx);
    }

    public void setFilled(int idx, boolean v, int fill) {
        filled.set(idx, v);
        fillColor.set(idx, fill);
    }

    public void remove(int idx) {
        if (idx < 0 || idx >= size()) return;
        types.remove(idx);
        geometry.remove(idx);
        strokeColor.remove(idx);
        thickness.remove(idx);
        style.remove(idx);
        filled.remove(idx);
        fillColor.remove(idx);
    }

    public void clear() {
        types.clear();
        geometry.clear();
        strokeColor.clear();
        thickness.clear();
        style.clear();
        filled.clear();
        fillColor.clear();
    }


    public void addLine(Line line) {
        types.add(OBJ_LINE);
        geometry.add(line);
        strokeColor.add(line.getColor());
        thickness.add(line.getThickness());
        style.add(line.getStyle());
        filled.add(false);
        fillColor.add(0);
    }

    public void addRect(int x1, int y1, int x2, int y2, int stroke, int thick, int st, boolean doFill, int fill) {
        types.add(OBJ_RECT);
        geometry.add(new int[]{x1, y1, x2, y2});
        strokeColor.add(stroke);
        thickness.add(Math.max(1, thick));
        style.add(st);
        filled.add(doFill);
        fillColor.add(fill);
    }

    public void addSquare(int x1, int y1, int x2, int y2, int stroke, int thick, int st, boolean doFill, int fill) {
        types.add(OBJ_SQUARE);
        geometry.add(new int[]{x1, y1, x2, y2});
        strokeColor.add(stroke);
        thickness.add(Math.max(1, thick));
        style.add(st);
        filled.add(doFill);
        fillColor.add(fill);
    }

    public void addCircle(int cx, int cy, int r, int stroke, int thick, int st, boolean doFill, int fill) {
        types.add(OBJ_CIRCLE);
        geometry.add(new int[]{cx, cy, r});
        strokeColor.add(stroke);
        thickness.add(Math.max(1, thick));
        style.add(st);
        filled.add(doFill);
        fillColor.add(fill);
    }

    public void addPolygon(List<Point> pts, int stroke, int thick, int st, boolean doFill, int fill) {
        types.add(OBJ_POLYGON);
        List<Point> copy = new ArrayList<>();
        for (Point p : pts) copy.add(new Point(p.getX(), p.getY()));
        geometry.add(copy);
        strokeColor.add(stroke);
        thickness.add(Math.max(1, thick));
        style.add(st);
        filled.add(doFill);
        fillColor.add(fill);
    }


    public void translate(int idx, int dx, int dy) {
        if (idx < 0 || idx >= size()) return;
        int t = types.get(idx);
        Object g = geometry.get(idx);
        if (t == OBJ_LINE) {
            Line l = (Line) g;
            l.setP1(new Point(l.getP1().getX() + dx, l.getP1().getY() + dy));
            l.setP2(new Point(l.getP2().getX() + dx, l.getP2().getY() + dy));
        } else if (t == OBJ_RECT || t == OBJ_SQUARE) {
            int[] a = (int[]) g;
            a[0] += dx; a[1] += dy; a[2] += dx; a[3] += dy;
        } else if (t == OBJ_CIRCLE) {
            int[] a = (int[]) g;
            a[0] += dx; a[1] += dy;
        } else if (t == OBJ_POLYGON) {
            List<Point> pts = (List<Point>) g;
            for (int i = 0; i < pts.size(); i++) {
                Point p = pts.get(i);
                pts.set(i, new Point(p.getX() + dx, p.getY() + dy));
            }
        }
    }

    public void updateStroke(int idx, int stroke, int thick, int st) {
        if (idx < 0 || idx >= size()) return;
        strokeColor.set(idx, stroke);
        thickness.set(idx, Math.max(1, thick));
        style.set(idx, st);

        if (types.get(idx) == OBJ_LINE) {
            Line l = (Line) geometry.get(idx);
            l.setColor(stroke);
            l.setThickness(thick);
            l.setStyle(st);
        }
    }
}
