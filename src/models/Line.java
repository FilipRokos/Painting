package models;


public class Line {

    private Point p1, p2;
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
    }

    public boolean isCorrectionMode() {
        return correctionMode;
    }

    public void setCorrectionMode(boolean correctionMode) {
        this.correctionMode = correctionMode;
    }


}
