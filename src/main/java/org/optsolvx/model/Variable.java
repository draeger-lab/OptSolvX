package org.optsolvx.model;

/**
 * Represents a single variable in the linear programming model.
 * Stores name, lower and upper bounds.
 * The index of a variable is managed by the parent LP model class (AbstractLPModel), not by this object.
 */
public class Variable {
    // Unique name of the variable (user-defined)
    private final String name;
    // Lower bound (inclusive)
    private final double lowerBound;
    // Upper bound (inclusive)
    private final double upperBound;

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

    /**
     * Returns a debug string with variable details.
     */
    @Override
    public String toString() {
        return String.format("Variable{name='%s', [%f, %f]}", name, lowerBound, upperBound);
    }
}
