package org.optsolvx.model;

import org.apache.commons.math3.optim.linear.Relationship;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a single linear constraint in the LP model.
 * Stores a map from variable names to their coefficients,
 * the constraint relationship (≤, ≥ or =) and the right-hand side value.
 * The index of a constraint is managed by the parent LP model class (AbstractLPModel),
 * not by this object.
 */
public class Constraint {
    /**
     * Unique, user-defined name of this constraint.
     */
    private final String name;

    public enum Relation {LEQ, GEQ, EQ}

    /**
     * Immutable map of variable names to their coefficients.
     */
    private final Map<String, Double> coefficients;

    private final Relation relation;

    /**
     * Right-hand side value of the constraint.
     */
    private final double rhs;


    /**
     * Constructs a new Constraint.
     *
     * @param name         unique name of the constraint
     * @param coefficients map of variable names to coefficients (will be wrapped immutable)
     * @param relation     type of constraint (LEQ, GEQ, EQ)
     * @param rhs          right-hand side constant
     */
    public Constraint(String name,
                      Map<String, Double> coefficients,
                      Relation relation,
                      double rhs) {
        this.name = name;
        this.coefficients = Collections.unmodifiableMap(coefficients);
        this.relation = relation;
        this.rhs = rhs;
    }

    /**
     * Returns the user-defined name of this constraint.
     *
     * @return constraint name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the immutable map of coefficients for each variable.
     *
     * @return variable→coefficient map
     */
    public Map<String, Double> getCoefficients() {
        return coefficients;
    }

    /**
     * Returns the right-hand side constant of this constraint.
     *
     * @return RHS value
     */
    public double getRhs() {
        return rhs;
    }

    /**
     * Returns the internal relation type (LEQ, GEQ, EQ).
     *
     * @return constraint relation enum
     */
    public Relation getRelation() {
        return relation;
    }

    /**
     * Returns the right-hand side value (alternative getter).
     *
     * @return right-hand side value
     */
    public double getRightHandSide() {
        return rhs;
    }

    /**
     * Returns a debug-friendly string of all constraint details.
     *
     * @return formatted constraint summary
     */
    @Override
    public String toString() {
        return String.format(
                "%s{name='%s', rel=%s, rhs=%s, coeffs=%s}",
                getClass().getSimpleName(), name, relation, rhs, coefficients
        );
    }
}
