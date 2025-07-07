package org.optsolvx.model;

import org.apache.commons.math3.optim.linear.Relationship;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a linear constraint in the LP model.
 */
public class Constraint {
    private final String name;
    private final Map<String, Double> coefficients; // variable name -> coefficient
    private final Relationship relationship;
    private final double rhs;
    private int index = -1; // assigned during build()

    public Constraint(String name, Map<String, Double> coefficients, Relationship relationship, double rhs) {
        this.name = name;
        this.coefficients = Collections.unmodifiableMap(coefficients); // immutable for safety
        this.relationship = relationship;
        this.rhs = rhs;
    }

    public String getName() { return name; }
    public Map<String, Double> getCoefficients() { return coefficients; }
    public Relationship getRelationship() { return relationship; }
    public double getRhs() { return rhs; }
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    @Override
    public String toString() {
        return String.format("Constraint{name='%s', relationship=%s, rhs=%f, index=%d, coeffs=%s}",
                name, relationship, rhs, index, coefficients);
    }
}
