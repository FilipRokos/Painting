package models;


public class Line {

    private Point p1, p2;
    private int style = 0;
    private int thickness = 1;
    private int color = 0xFFFF0000;

    private boolean isDotted = false;
    private boolean correctionMode = false;
    public Line(Point p1, Point p2) {
        this.p1 = p1;
        this.p2 = p2;

    }
    public Line(Point p1, Point p2,boolean isDotted) {
        this.p1 = p1;
        this.p2 = p2;
        this.isDotted = isDotted;
        this.style = isDotted ? 2 : 0;
    }
    public Point getP1() {
        return p1;
    }

    public void setP1(Point p1) {
        this.p1 = p1;
    }

    public Point getP2() {
        return p2;
    }

    public void setP2(Point p2) {
        this.p2 = p2;
    }

    public boolean isDotted() {
        return isDotted;
    }

    public void setDotted(boolean dotted) {
        isDotted = dotted;
        if (dotted) this.style = 2;
    }

    public int getStyle() {
        return isDotted ? 2 : style;
    }

    public void setStyle(int style) {
        this.style = style;
        this.isDotted = (style == 2);
    }

    public int getThickness() {
        return Math.max(1, thickness);
    }

    public void setThickness(int thickness) {
        this.thickness = Math.max(1, thickness);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isCorrectionMode() {
        return correctionMode;
    }

    public void setCorrectionMode(boolean correctionMode) {
        this.correctionMode = correctionMode;
    }


}
