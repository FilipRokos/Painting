import models.LineCanvas;
import models.Point;
import models.Line;
import rasterizers.LineCanvasRasterizer;
import rasterizers.TrivialRasterizer;
import rasters.Raster;
import rasters.RasterBufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class App {


    private static final int TOOL_SELECT = 0;
    private static final int TOOL_LINE = 1;
    private static final int TOOL_RECT = 2;
    private static final int TOOL_SQUARE = 3;
    private static final int TOOL_CIRCLE = 4;
    private static final int TOOL_POLYGON = 5;
    private static final int TOOL_ERASER = 6;
    private static final int TOOL_FILL = 7;

    private static final int STYLE_SOLID = 0;
    private static final int STYLE_DASHED = 1;
    private static final int STYLE_DOTTED = 2;

    private final JPanel panel;
    private final Raster raster;
    private final RasterBufferedImage paintLayer; // persistent pixels (fill/eraser)

    private final TrivialRasterizer rasterizer;
    private final LineCanvas canvas;
    private final LineCanvasRasterizer canvasRasterizer;

    private MouseAdapter mouse;

    // UI state
    private int tool = TOOL_LINE;
    private int strokeColor = Color.CYAN.getRGB();
    private int fillColor = new Color(255, 220, 120).getRGB();
    private int thickness = 1;
    private int style = STYLE_SOLID;

    // drawing state
    private Point anchor = null;              // first click / anchor point
    private Point lastMouse = null;
    private boolean lastCtrl = false;
    private boolean lastShift = false;
    private final List<Point> polygonDraft = new ArrayList<>();

    // selection/edit state
    private int selected = -1;
    private int activeHandle = -1;            // -1 none, otherwise depends on type
    private Point lastDrag = null;

    private int backgroundColor = 0xFFAAAAAA;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App(1280, 720).start());
    }

    public App(int width, int height) {
        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.setTitle("Painting");
        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        raster = new RasterBufferedImage(width, height);
        paintLayer = new RasterBufferedImage(width, height);
        paintLayer.setClearColor(backgroundColor);
        paintLayer.clear();

        panel = new JPanel() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                raster.repaint(g);
            }
        };
        panel.setPreferredSize(new Dimension(width, height));

        rasterizer = new TrivialRasterizer(Color.CYAN, raster);
        canvas = new LineCanvas();
        canvasRasterizer = new LineCanvasRasterizer(rasterizer);

        frame.add(buildTopPanel(), BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        createMouse();
        panel.addMouseListener(mouse);
        panel.addMouseMotionListener(mouse);

        installKeybinds();
        redraw(null);
    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        String[] tools = {"Select/Move", "Line", "Rectangle", "Square", "Circle", "Polygon", "Eraser", "Fill"};
        JComboBox<String> toolBox = new JComboBox<>(tools);
        toolBox.setSelectedIndex(tool);
        toolBox.addActionListener(e -> {
            tool = toolBox.getSelectedIndex();
            cancelDraft();
        });

        String[] styles = {"Solid", "Dashed", "Dotted"};
        JComboBox<String> styleBox = new JComboBox<>(styles);
        styleBox.setSelectedIndex(style);
        styleBox.addActionListener(e -> style = styleBox.getSelectedIndex());

        JSlider thick = new JSlider(1, 12, thickness);
        thick.setPreferredSize(new Dimension(120, 20));
        thick.addChangeListener(e -> thickness = thick.getValue());

        JButton strokeBtn = new JButton("Stroke");
        strokeBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(panel, "Choose stroke color", new Color(strokeColor, true));
            if (c != null) strokeColor = c.getRGB();
            if (selected >= 0) {
                canvas.updateStroke(selected, strokeColor, thickness, style);
                redraw(null);
            }
        });

        JButton fillBtn = new JButton("Fill");
        fillBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(panel, "Choose fill color", new Color(fillColor, true));
            if (c != null) fillColor = c.getRGB();
        });

        int[] pal = {
                Color.BLACK.getRGB(), Color.WHITE.getRGB(), Color.RED.getRGB(), Color.GREEN.getRGB(),
                Color.BLUE.getRGB(), Color.YELLOW.getRGB(), Color.MAGENTA.getRGB(), Color.CYAN.getRGB()
        };
        JPanel palette = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        for (int rgb : pal) {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(18, 18));
            b.setBackground(new Color(rgb, true));
            b.addActionListener(e -> {
                strokeColor = rgb;
                if (selected >= 0) {
                    canvas.updateStroke(selected, strokeColor, thickness, style);
                    redraw(null);
                }
            });
            palette.add(b);
        }

        top.add(new JLabel("Tool:"));
        top.add(toolBox);
        top.add(new JLabel("Style:"));
        top.add(styleBox);
        top.add(new JLabel("Thickness:"));
        top.add(thick);
        top.add(strokeBtn);
        top.add(fillBtn);
        top.add(new JLabel("Palette:"));
        top.add(palette);
        top.add(new JLabel("   CTRL=dotted, SHIFT=snap, DEL=delete selected, C=clear"));

        return top;
    }
    private Point snap(Point p1, Point p2) {
        int x1 = p1.getX(), y1 = p1.getY();
        int x2 = p2.getX(), y2 = p2.getY();

        int dx = x2 - x1;
        int dy = y2 - y1;

        int adx = Math.abs(dx);
        int ady = Math.abs(dy);

        double ratio = 2.0;

        if (adx >= ratio * ady) {
            return new Point(x2, y1);
        } else if (ady >= ratio * adx) {
            return new Point(x1, y2);
        } else {                           // 45° / -45°
            int sx = Integer.compare(dx, 0);
            int sy = Integer.compare(dy, 0);
            int m = Math.max(adx, ady);
            return new Point(x1 + sx * m, y1 + sy * m);
        }
    }
    private void installKeybinds() {
        panel.setFocusable(true);
        panel.requestFocusInWindow();

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('c'), "clearAll");
        panel.getActionMap().put("clearAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
        panel.getActionMap().put("deleteSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selected >= 0) {
                    canvas.remove(selected);
                    selected = -1;
                    redraw(null);
                }
            }
        });

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelDraft");
        panel.getActionMap().put("cancelDraft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelDraft();
            }
        });
    }

    private void clearAll() {
        canvas.clear();
        selected = -1;
        cancelDraft();
        paintLayer.setClearColor(backgroundColor);
        paintLayer.clear();
        redraw(null);
    }

    private void cancelDraft() {
        anchor = null;
        polygonDraft.clear();
        activeHandle = -1;
        lastDrag = null;
        redraw(null);
    }

    private void redraw(Line previewLine) {
        raster.setClearColor(backgroundColor);
        raster.clear();

        Graphics g = raster.getGraphics();
        g.drawImage(paintLayer.getImg(), 0, 0, null);

        canvasRasterizer.rasterizeCanvas(canvas);

        if (previewLine != null) {
            rasterizer.rasterize(previewLine);
        } else {
            drawPreviewShape();
        }

        drawSelectionHandles();

        panel.repaint();
    }

    private void drawPreviewShape() {
        if (anchor == null || lastMouse == null) return;

        int effectiveStyle = lastCtrl ? STYLE_DOTTED : style;
        boolean snap = lastShift;

        if (tool == TOOL_RECT || tool == TOOL_SQUARE) {
            int x1 = anchor.getX(), y1 = anchor.getY();
            int x2 = lastMouse.getX(), y2 = lastMouse.getY();
            if (tool == TOOL_SQUARE) {
                int dx = x2 - x1;
                int dy = y2 - y1;
                int s = Math.max(Math.abs(dx), Math.abs(dy));
                x2 = x1 + Integer.signum(dx) * s;
                y2 = y1 + Integer.signum(dy) * s;
            }
            rasterizer.rasterizeRectangle(x1, y1, x2, y2, strokeColor, thickness, effectiveStyle, snap);
        }
        else if (tool == TOOL_CIRCLE) {
            int dx = lastMouse.getX() - anchor.getX();
            int dy = lastMouse.getY() - anchor.getY();
            int r = (int) Math.round(Math.sqrt(dx * (double) dx + dy * (double) dy));
            rasterizer.rasterizeCircle(anchor.getX(), anchor.getY(), r, strokeColor, thickness, effectiveStyle);
        }
        else if (tool == TOOL_POLYGON) {
            if (polygonDraft.isEmpty()) return;

            for (int i = 0; i < polygonDraft.size() - 1; i++) {
                Line edge = new Line(polygonDraft.get(i), polygonDraft.get(i + 1));
                edge.setColor(strokeColor);
                edge.setThickness(thickness);
                edge.setStyle(effectiveStyle);
                edge.setCorrectionMode(false);
                rasterizer.rasterize(edge);
            }

            if (polygonDraft.size() >= 3) {
                Line closing = new Line(
                        polygonDraft.get(polygonDraft.size() - 1),
                        polygonDraft.get(0)
                );
                closing.setColor(strokeColor);
                closing.setThickness(thickness);
                closing.setStyle(effectiveStyle);
                closing.setCorrectionMode(false);
                rasterizer.rasterize(closing);
            }


        }
    }
    private boolean clickOnPolygonEdge(Point p, List<Point> pts, double maxDist) {
        if (pts.size() < 3) return false;

        for (int i = 0; i < pts.size(); i++) {
            Point a = pts.get(i);
            Point b = pts.get((i + 1) % pts.size());
            if (distPointToSegment(p, a, b) <= maxDist) {
                return true;
            }
        }
        return false;
    }
    private void drawSelectionHandles() {
        if (selected < 0 || selected >= canvas.size()) return;

        int type = canvas.getType(selected);
        int c = Color.ORANGE.getRGB();

        if (type == LineCanvas.OBJ_LINE) {
            Line l = (Line) canvas.getGeometry(selected);
            drawHandle(l.getP1(), c);
            drawHandle(l.getP2(), c);
        }
        else if (type == LineCanvas.OBJ_RECT || type == LineCanvas.OBJ_SQUARE) {
            int[] a = (int[]) canvas.getGeometry(selected);
            drawHandle(new Point(a[0], a[1]), c);
            drawHandle(new Point(a[2], a[1]), c);
            drawHandle(new Point(a[2], a[3]), c);
            drawHandle(new Point(a[0], a[3]), c);
        }
        else if (type == LineCanvas.OBJ_CIRCLE) {
            int[] a = (int[]) canvas.getGeometry(selected);
            drawHandle(new Point(a[0], a[1]), c);
            drawHandle(new Point(a[0] + a[2], a[1]), c); // radius handle
        }
        else if (type == LineCanvas.OBJ_POLYGON) {
            @SuppressWarnings("unchecked")
            List<Point> pts = (List<Point>) canvas.getGeometry(selected);
            for (Point p : pts) drawHandle(p, c);
        }
    }

    private void drawHandle(Point p, int color) {
        int x = p.getX();
        int y = p.getY();
        for (int yy = y - 2; yy <= y + 2; yy++) {
            for (int xx = x - 2; xx <= x + 2; xx++) {
                raster.setPixel(xx, yy, color);
            }
        }
    }

    private static boolean near(Point a, Point b, int radiusPx) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return dx * dx + dy * dy <= radiusPx * radiusPx;
    }

    private static double distPointToSegment(Point p, Point a, Point b) {
        double px = p.getX(), py = p.getY();
        double ax = a.getX(), ay = a.getY();
        double bx = b.getX(), by = b.getY();
        double abx = bx - ax, aby = by - ay;
        double apx = px - ax, apy = py - ay;
        double ab2 = abx * abx + aby * aby;
        double t = (ab2 == 0) ? 0 : (apx * abx + apy * aby) / ab2;
        t = Math.max(0, Math.min(1, t));
        double cx = ax + t * abx;
        double cy = ay + t * aby;
        double dx = px - cx;
        double dy = py - cy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean pointInPolygon(Point p, List<Point> poly) {
        boolean inside = false;
        int n = poly.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            int xi = poly.get(i).getX(), yi = poly.get(i).getY();
            int xj = poly.get(j).getX(), yj = poly.get(j).getY();
            boolean intersect = ((yi > p.getY()) != (yj > p.getY())) &&
                    (p.getX() < (xj - xi) * (p.getY() - yi) / (double) (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    private int pickObject(Point p) {
        for (int i = canvas.size() - 1; i >= 0; i--) {
            int type = canvas.getType(i);
            Object g = canvas.getGeometry(i);

            if (type == LineCanvas.OBJ_LINE) {
                Line l = (Line) g;
                if (distPointToSegment(p, l.getP1(), l.getP2()) <= 6) return i;
            }
            else if (type == LineCanvas.OBJ_RECT || type == LineCanvas.OBJ_SQUARE) {
                int[] a = (int[]) g;
                int x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
                int left = Math.min(x1, x2), right = Math.max(x1, x2);
                int top = Math.min(y1, y2), bottom = Math.max(y1, y2);
                boolean onEdge =
                        distPointToSegment(p, new Point(left, top), new Point(right, top)) <= 6 ||
                        distPointToSegment(p, new Point(right, top), new Point(right, bottom)) <= 6 ||
                        distPointToSegment(p, new Point(right, bottom), new Point(left, bottom)) <= 6 ||
                        distPointToSegment(p, new Point(left, bottom), new Point(left, top)) <= 6;
                boolean inside = (p.getX() >= left && p.getX() <= right && p.getY() >= top && p.getY() <= bottom);
                if (onEdge || inside) return i;
            }
            else if (type == LineCanvas.OBJ_CIRCLE) {
                int[] a = (int[]) g;
                int cx = a[0], cy = a[1], r = a[2];
                int dx = p.getX() - cx;
                int dy = p.getY() - cy;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (Math.abs(d - r) <= 6 || d <= r) return i;
            }
            else if (type == LineCanvas.OBJ_POLYGON) {
                @SuppressWarnings("unchecked")
                List<Point> pts = (List<Point>) g;
                if (pts.size() >= 2) {
                    for (int k = 0; k < pts.size(); k++) {
                        Point a = pts.get(k);
                        Point b = pts.get((k + 1) % pts.size());
                        if (distPointToSegment(p, a, b) <= 6) return i;
                    }
                }
                if (pts.size() >= 3 && pointInPolygon(p, pts)) return i;
            }
        }
        return -1;
    }

    private int pickHandle(int objIdx, Point p) {
        if (objIdx < 0) return -1;
        int type = canvas.getType(objIdx);
        if (type == LineCanvas.OBJ_LINE) {
            Line l = (Line) canvas.getGeometry(objIdx);
            if (near(p, l.getP1(), 6)) return 0;
            if (near(p, l.getP2(), 6)) return 1;
        }
        else if (type == LineCanvas.OBJ_RECT || type == LineCanvas.OBJ_SQUARE) {
            int[] a = (int[]) canvas.getGeometry(objIdx);
            Point[] corners = {
                    new Point(a[0], a[1]), new Point(a[2], a[1]), new Point(a[2], a[3]), new Point(a[0], a[3])
            };
            for (int i = 0; i < corners.length; i++) if (near(p, corners[i], 7)) return i;
        }
        else if (type == LineCanvas.OBJ_CIRCLE) {
            int[] a = (int[]) canvas.getGeometry(objIdx);
            if (near(p, new Point(a[0], a[1]), 7)) return 0; // center move
            if (near(p, new Point(a[0] + a[2], a[1]), 7)) return 1; // radius
        }
        else if (type == LineCanvas.OBJ_POLYGON) {
            @SuppressWarnings("unchecked")
            List<Point> pts = (List<Point>) canvas.getGeometry(objIdx);
            for (int i = 0; i < pts.size(); i++) if (near(p, pts.get(i), 7)) return i;
        }
        return -1;
    }

    private void createMouse() {
        mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouse = new Point(e.getX(), e.getY());
                lastCtrl = e.isControlDown();
                lastShift = e.isShiftDown();

                // preview
                if (anchor == null) return;
                Line preview = buildPreviewLine(e);
                redraw(preview);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                panel.requestFocusInWindow();
                Point p = new Point(e.getX(), e.getY());
                lastCtrl = e.isControlDown();
                lastShift = e.isShiftDown();

                if (tool == TOOL_SELECT) {
                    selected = pickObject(p);
                    activeHandle = pickHandle(selected, p);
                    lastDrag = p;
                    redraw(null);
                    return;
                }

                if (tool == TOOL_ERASER) {
                    lastDrag = p;
                    eraseAt(p, e.isShiftDown() ? 18 : 10);
                    return;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = new Point(e.getX(), e.getY());
                lastCtrl = e.isControlDown();
                lastShift = e.isShiftDown();

                if (tool == TOOL_SELECT) {
                    if (selected < 0 || lastDrag == null) return;
                    int dx = p.getX() - lastDrag.getX();
                    int dy = p.getY() - lastDrag.getY();

                    if (activeHandle < 0) {
                        canvas.translate(selected, dx, dy);
                    } else {
                        resizeSelectedByHandle(selected, activeHandle, p);
                    }

                    lastDrag = p;
                    redraw(null);
                    return;
                }

                if (tool == TOOL_ERASER) {
                    eraseAt(p, e.isShiftDown() ? 18 : 10);
                    lastDrag = p;
                    return;
                }

                lastMouse = p;
                if (anchor != null) {
                    redraw(buildPreviewLine(e));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (tool == TOOL_SELECT) {
                    lastDrag = null;
                    activeHandle = -1;
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                Point p = new Point(e.getX(), e.getY());
                lastCtrl = e.isControlDown();
                lastShift = e.isShiftDown();

                if (tool == TOOL_FILL) {
                    int hit = pickObject(p);
                    if (hit >= 0) {
                        canvas.setFilled(hit, true, fillColor);
                        redraw(null);
                    } else {
                        floodFillToLayer(p, fillColor);
                    }
                    return;
                }

                if (tool == TOOL_SELECT || tool == TOOL_ERASER) return;

                if (tool == TOOL_POLYGON) {
                    handlePolygonClick(e, p);
                    return;
                }

                if (anchor == null) {
                    anchor = p;
                    redraw(null);
                    return;
                }

                commitShape(e, anchor, p);
                anchor = null;
                redraw(null);
            }
        };
    }

    private void resizeSelectedByHandle(int idx, int handle, Point p) {
        int type = canvas.getType(idx);
        Object g = canvas.getGeometry(idx);

        if (type == LineCanvas.OBJ_LINE) {
            Line l = (Line) g;
            if (handle == 0) l.setP1(p);
            if (handle == 1) l.setP2(p);
        }
        else if (type == LineCanvas.OBJ_RECT || type == LineCanvas.OBJ_SQUARE) {
            int[] a = (int[]) g;
            if (handle == 0) { a[0] = p.getX(); a[1] = p.getY(); }
            if (handle == 1) { a[2] = p.getX(); a[1] = p.getY(); }
            if (handle == 2) { a[2] = p.getX(); a[3] = p.getY(); }
            if (handle == 3) { a[0] = p.getX(); a[3] = p.getY(); }
        }
        else if (type == LineCanvas.OBJ_CIRCLE) {
            int[] a = (int[]) g;
            if (handle == 0) { a[0] = p.getX(); a[1] = p.getY(); }
            if (handle == 1) {
                int dx = p.getX() - a[0];
                int dy = p.getY() - a[1];
                a[2] = (int) Math.round(Math.sqrt(dx * (double) dx + dy * (double) dy));
            }
        }
        else if (type == LineCanvas.OBJ_POLYGON) {
            @SuppressWarnings("unchecked")
            List<Point> pts = (List<Point>) g;
            if (handle >= 0 && handle < pts.size()) pts.set(handle, p);
        }
    }

    private Line buildPreviewLine(MouseEvent e) {
        if (anchor == null || lastMouse == null) return null;

        int effectiveStyle = e.isControlDown() ? STYLE_DOTTED : style;
        boolean snap = e.isShiftDown();

        if (tool == TOOL_LINE) {
            Line l = new Line(anchor, lastMouse);
            l.setColor(strokeColor);
            l.setThickness(thickness);
            l.setStyle(effectiveStyle);
            l.setCorrectionMode(snap);
            return l;
        }
        if (tool == TOOL_POLYGON) {
            return null;
        }


        return null;
    }

    private void commitShape(MouseEvent e, Point a, Point b) {
        int effectiveStyle = e.isControlDown() ? STYLE_DOTTED : style;

        if (tool == TOOL_LINE) {
            Line l = new Line(a, b);
            l.setColor(strokeColor);
            l.setThickness(thickness);
            l.setStyle(effectiveStyle);
            l.setCorrectionMode(e.isShiftDown());
            canvas.addLine(l);
            return;
        }

        if (tool == TOOL_RECT) {
            canvas.addRect(a.getX(), a.getY(), b.getX(), b.getY(), strokeColor, thickness, effectiveStyle, false, 0);
            return;
        }

        if (tool == TOOL_SQUARE) {
            canvas.addSquare(a.getX(), a.getY(), b.getX(), b.getY(), strokeColor, thickness, effectiveStyle, false, 0);
            return;
        }

        if (tool == TOOL_CIRCLE) {
            int dx = b.getX() - a.getX();
            int dy = b.getY() - a.getY();
            int r = (int) Math.round(Math.sqrt(dx * (double) dx + dy * (double) dy));
            canvas.addCircle(a.getX(), a.getY(), r, strokeColor, thickness, effectiveStyle, false, 0);
        }
    }
    private int findNearestEdgeStartIndex(Point p, List<Point> pts) {
        if (pts.size() < 2) return -1;

        int bestIdx = 0;
        double bestDist = Double.MAX_VALUE;

        int edgeCount = (pts.size() < 3) ? pts.size() - 1 : pts.size();

        for (int i = 0; i < edgeCount; i++) {
            Point a = pts.get(i);
            Point b = pts.get((i + 1) % pts.size());
            double d = distPointToSegment(p, a, b);

            if (d < bestDist) {
                bestDist = d;
                bestIdx = i;
            }
        }
        return bestIdx;
    }
    private void handlePolygonClick(MouseEvent e, Point p) {
        int effectiveStyle = e.isControlDown() ? STYLE_DOTTED : style;
        boolean snapMode = e.isShiftDown();

        if (polygonDraft.isEmpty()) {
            polygonDraft.add(p);
            anchor = p;
            redraw(null);
            return;
        }

        if (polygonDraft.size() >= 3 && clickOnPolygonEdge(p, polygonDraft, 6.0)) {
            canvas.addPolygon(new ArrayList<>(polygonDraft), strokeColor, thickness, effectiveStyle, false, 0);
            polygonDraft.clear();
            anchor = null;
            redraw(null);
            return;
        }

        if (polygonDraft.size() == 1) {
            Point base = polygonDraft.get(0);
            Point pp = snapMode ? snap(base, p) : p;
            polygonDraft.add(pp);
            anchor = polygonDraft.get(0);
            redraw(null);
            return;
        }

        int edgeIdx = findNearestEdgeStartIndex(p, polygonDraft);
        Point a = polygonDraft.get(edgeIdx);

        Point pp = snapMode ? snap(a, p) : p;

        polygonDraft.add(edgeIdx + 1, pp);
        anchor = polygonDraft.get(0);
        redraw(null);
    }

    private void eraseAt(Point p, int radius) {
        int x0 = p.getX();
        int y0 = p.getY();
        int r2 = radius * radius;
        for (int y = y0 - radius; y <= y0 + radius; y++) {
            for (int x = x0 - radius; x <= x0 + radius; x++) {
                int dx = x - x0;
                int dy = y - y0;
                if (dx * dx + dy * dy <= r2) {
                    paintLayer.setPixel(x, y, backgroundColor);
                }
            }
        }
        redraw(null);
    }

    private void floodFillToLayer(Point start, int newColor) {
        int w = paintLayer.getWidth();
        int h = paintLayer.getHeight();
        int x0 = start.getX();
        int y0 = start.getY();
        if (x0 < 0 || y0 < 0 || x0 >= w || y0 >= h) return;

        redraw(null);
        int target = raster.getPixel(x0, y0);
        if (target == newColor) return;

        boolean[] vis = new boolean[w * h];
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{x0, y0});
        vis[y0 * w + x0] = true;

        while (!q.isEmpty()) {
            int[] cur = q.removeFirst();
            int x = cur[0], y = cur[1];
            if (raster.getPixel(x, y) != target) continue;

            paintLayer.setPixel(x, y, newColor);

            if (x > 0 && !vis[y * w + (x - 1)]) { vis[y * w + (x - 1)] = true; q.add(new int[]{x - 1, y}); }
            if (x + 1 < w && !vis[y * w + (x + 1)]) { vis[y * w + (x + 1)] = true; q.add(new int[]{x + 1, y}); }
            if (y > 0 && !vis[(y - 1) * w + x]) { vis[(y - 1) * w + x] = true; q.add(new int[]{x, y - 1}); }
            if (y + 1 < h && !vis[(y + 1) * w + x]) { vis[(y + 1) * w + x] = true; q.add(new int[]{x, y + 1}); }
        }

        redraw(null);
    }

    public void start() {
        redraw(null);
    }
}
