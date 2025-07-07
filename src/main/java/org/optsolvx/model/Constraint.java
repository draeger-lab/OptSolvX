package org.optsolvx.model;

import org.apache.commons.math3.optim.linear.Relationship;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a single linear constraint in the LP model.
 * Maps variable names to coefficients, stores relationship and right-hand side.
 */
public class Constraint {
    // Unique name of the constraint (user-defined)
    private final String name;
    // Map from variable name to coefficient in the constraint
    private final Map<String, Double> coefficients;
    // Type of relationship: LEQ (<=), GEQ (>=), EQ (=)
    private final Relationship relationship;
    // Right-hand side of the constraint
    private final double rhs;
    // Internal index (set by model during build)
    private int index = -1;

    /**
     * Creates a new constraint.
     * @param name unique constraint name
     * @param coefficients map of variable names to coefficients
     * @param relationship constraint type (LEQ, GEQ, EQ)
     * @param rhs right-hand side value
     */
    public Constraint(String name, Map<String, Double> coefficients, Relationship relationship, double rhs) {
        this.name = name;
        this.coefficients = Collections.unmodifiableMap(coefficients); // immutable for safety
        this.relationship = relationship;
        this.rhs = rhs;
    }

    // @return constraint name
    public String getName() { return name; }
    // @return coefficients map
    public Map<String, Double> getCoefficients() { return coefficients; }
    // @return relationship type
    public Relationship getRelationship() { return relationship; }
    // @return right-hand side
    public double getRhs() { return rhs; }
    // @return internal index
    public int getIndex() { return index; }
    // Sets the internal index (used by model)
    public void setIndex(int index) { this.index = index; }

    /**
     * Returns a debug string with constraint details.
     */
    @Override
    public String toString() {
        return String.format("Constraint{name='%s', relationship=%s, rhs=%f, index=%d, coeffs=%s}",
                name, relationship, rhs, index, coefficients);
    }
}
