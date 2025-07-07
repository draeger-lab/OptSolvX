package org.optsolvx.model;

/**
 * Represents a single variable in the LP model.
 */
public class Variable {
    private final String name;
    private final double lowerBound;
    private final double upperBound;
    private int index = -1; // assigned during build()

    public Variable(String name, double lowerBound, double upperBound) {
        this.name = name;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public String getName() { return name; }
    public double getLowerBound() { return lowerBound; }
    public double getUpperBound() { return upperBound; }
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    @Override
    public String toString() {
        return String.format("Variable{name='%s', [%f, %f], index=%d}", name, lowerBound, upperBound, index);
    }
}
