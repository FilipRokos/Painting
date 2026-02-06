import models.LineCanvas;
import models.Point;
import models.Line;
import rasterizers.LineCanvasRasterizer;
import rasterizers.TrivialRasterizer;
import rasters.Raster;
import rasters.RasterBufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;

public class App {

    private final JPanel panel;
    private final Raster raster;
    private final TrivialRasterizer rasterizer;
    private MouseAdapter mouseAdapter;
    private Point mousePosition1;
    private final LineCanvas lineCanvas;
    private final LineCanvasRasterizer lineRasterizer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App(2560, 1440).start());
    }

    public void clear(int color) {
        raster.setClearColor(color);
        raster.clear();
    }

    public void present(Graphics graphics) {
        raster.repaint(graphics);
    }

    public void start() {
        clear(0xaaaaaa);
        panel.repaint();
    }

    public App(int width, int height) {
        JFrame frame = new JFrame();

        frame.setLayout(new BorderLayout());
        frame.setTitle("Painting");
        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        raster = new RasterBufferedImage(width, height);

        panel = new JPanel() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                present(g);
            }
        };

        panel.setPreferredSize(new Dimension(width, height));

        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        rasterizer = new TrivialRasterizer(Color.CYAN, raster);

        lineCanvas = new LineCanvas();
        lineRasterizer = new LineCanvasRasterizer(rasterizer);

        createMouseAdapters();
        panel.addMouseListener(mouseAdapter);
        panel.addMouseMotionListener(mouseAdapter);
    }

    private void createMouseAdapters() {

        mouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                mousePosition1 = new Point(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (mousePosition1 == null) return;

                Point mousePosition2 = new Point(e.getX(), e.getY());
                Line line = new Line(mousePosition1, mousePosition2);

                line.setDotted(e.isControlDown());
                line.setCorrectionMode(e.isShiftDown());

                raster.clear();
                lineRasterizer.rasterizeCanvas(lineCanvas);
                rasterizer.rasterize(line);
                panel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (mousePosition1 == null) return;

                Point mousePosition2 = new Point(e.getX(), e.getY());
                Line line = new Line(mousePosition1, mousePosition2);

                line.setDotted(e.isControlDown());
                line.setCorrectionMode(e.isShiftDown());

                lineCanvas.addLine(line);

                mousePosition1 = null; // DŮLEŽITÉ

                raster.clear();
                lineRasterizer.rasterizeCanvas(lineCanvas);
                panel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mousePosition1 = null;
            }

        };
    }
}
