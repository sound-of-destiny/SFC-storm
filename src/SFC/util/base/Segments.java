package util.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Segments implements Comparable<Segments> {
    private int begin;
    private int end;
    private double meanValue;
    private double mergeCost;
    private double segError;
    private int isFirst;
    private int indexloc;
    private int isatisfied;

    public Segments() {
        super();
        this.mergeCost = Double.MAX_VALUE;
        this.isFirst = 0;
        this.indexloc = 0;
        this.segError = 0;
    }

    public Segments(int begin, int end, double mergecost) {
        super();
        this.begin = begin;
        this.end = end;
        this.indexloc = 0;
        this.mergeCost = mergecost;
        this.segError = 0;
        System.out.println("----------------------------------");
        System.out.println("BeginP: " + begin + " EndP: " + end);
        System.out.println("----------------------------------");

    }

    public Segments(int begin, int end, double mergecost, double segError) {
        super();
        this.begin = begin;
        this.end = end;
        this.indexloc = 0;
        this.mergeCost = mergecost;
        this.segError = segError;
    }

    public Segments(int begin, int end, double mergecost, double segError, int isFirst) {
        super();
        this.begin = begin;
        this.end = end;
        this.indexloc = 0;
        this.mergeCost = mergecost;
        this.segError = segError;
        this.isFirst = isFirst;
    }

    public Segments(int begin, int end) {
        super();
        this.begin = begin;
        this.end = end;
        this.mergeCost = 0;
        this.isFirst = 1;
        this.indexloc = 0;
        this.segError = 0;
    }

    public Segments(int begin, int end, int spit) {
        super();
        this.begin = begin;
        this.end = end;
        this.mergeCost = 0;
        this.isFirst = spit;
        this.indexloc = 0;
        this.segError = 0;
    }

    public int getIndexloc() {
        return indexloc;
    }

    public void setIndexloc(int indexloc) {
        this.indexloc = indexloc;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public double getMegerCost() {
        return mergeCost;
    }

    public void setMegerCost(double mergecost) {
        this.mergeCost = mergecost;
    }

    public int getIsFirst() {
        return isFirst;
    }

    public void setIsFirst(int first) {
        this.isFirst = first;
    }

    public double getMeanValue() {
        return meanValue;
    }

    public void setMeanValue(double meanValue) {
        this.meanValue = meanValue;
    }

    public int getIsatisfied() {
        return isatisfied;
    }

    public void setIsatisfied(int isatisfied) {
        this.isatisfied = isatisfied;
    }

    public double getSegError() {
        return segError;
    }

    public void setSegError(double segError) {
        this.segError = segError;
    }

    // 按照begin参数，从小到大排列
    @Override
    public int compareTo(Segments o) {
        return this.getBegin() - o.getBegin() > 0 ? 1 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Segments segments = (Segments) o;
        return begin == segments.begin &&
                end == segments.end;
    }

    @Override
    public int hashCode() {

        return Objects.hash(begin, end);
    }

    public static void main(String[] args) {
        List<Segments> segments = new ArrayList<Segments>();
        segments.add(new Segments(5, 2));
        segments.add(new Segments(4, 2));
        segments.add(new Segments(2, 2));
        segments.add(new Segments(3, 2));
        segments.add(new Segments(9, 2));
        segments.add(new Segments(5, 6));

        Collections.sort(segments);
        for(Segments segment : segments) {
            System.out.println(segment.getBegin() + " , " + segment.getEnd());
        }
    }

    @Override
    public String toString() {
        return "Segments{" +
                "begin=" + (begin) +
                ", end=" + (end) +
                '}';
    }
}
