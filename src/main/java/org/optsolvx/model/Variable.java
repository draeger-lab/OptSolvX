package org.optsolvx.model;

/**
 * Represents a single variable in the linear programming model.
 * Stores name, lower and upper bounds, and internal index for solver mapping.
 */
public class Variable {
    // Unique name of the variable (user-defined)
    private final String name;
    // Lower bound (inclusive)
    private final double lowerBound;
    // Upper bound (inclusive)
    private final double upperBound;
    // Internal index (set by model during build)
    private int index = -1;

    /**
     * Creates a new variable with the given name and bounds.
     * @param name unique name
     * @param lowerBound lower bound (inclusive)
     * @param upperBound upper bound (inclusive)
     */
    public Variable(String name, double lowerBound, double upperBound) {
        this.name = name;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    // @return variable name
    public String getName() { return name; }
    // @return lower bound
    public double getLowerBound() { return lowerBound; }
    // @return upper bound
    public double getUpperBound() { return upperBound; }
    // @return internal index
    public int getIndex() { return index; }
    // Sets the internal index (used by model)
    public void setIndex(int index) { this.index = index; }

    /**
     * Returns a debug string with variable details.
     */
    @Override
    public String toString() {
        return String.format("Variable{name='%s', [%f, %f], index=%d}", name, lowerBound, upperBound, index);
    }
}
