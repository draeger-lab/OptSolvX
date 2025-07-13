package org.optsolvx.model;

import org.apache.commons.math3.optim.linear.Relationship;
import java.util.*;

/**
 * Generic base class for linear programming models (LP).
 * Stores all variables, constraints, and the objective function.
 * Can be used independently of any solver backend.
 */
public class AbstractLPModel {

    // List of all variables in the model, in insertion order
    private final List<Variable> variables = new ArrayList<>();

    // List of all constraints in the model
    private final List<Constraint> constraints = new ArrayList<>();

    // Maps variable names to their index in the variables list
    private final Map<String, Integer> varNameToIndex = new HashMap<>();

    // Maps constraint names to their index in the constraints list
    private final Map<String, Integer> constraintNameToIndex = new HashMap<>();

    // Coefficients of the objective function: varName -> coefficient
    private final Map<String, Double> objectiveCoefficients = new HashMap<>();

    // If true, model is a maximization problem; if false, minimization
    private boolean maximize = true;

    // True after build() is called; no further changes allowed
    private boolean built = false;

    /**
     * Throws if model has already been built.
     */
    private void checkNotBuilt() {
        if (built) {
            throw new IllegalStateException("Model already built; no modifications allowed.");
        }
    }

    /**
     * Adds a new variable to the model.
     *
     * @param name unique name of the variable
     * @param lower lower bound (inclusive)
     * @param upper upper bound (inclusive)
     * @return index of the variable in the variables list
     * @throws IllegalArgumentException if the name already exists
     * @throws IllegalStateException if the model is already built
     */
    public int addVariable(String name, double lower, double upper) {
        checkNotBuilt();
        if (varNameToIndex.containsKey(name)) {
            throw new IllegalArgumentException("Variable name already exists: " + name);
        }
        Variable var = new Variable(name, lower, upper);
        int idx = variables.size();
        variables.add(var);
        varNameToIndex.put(name, idx);
        return idx;
    }

    /**
     * Adds a new linear constraint to the model.
     *
     * @param name unique name of the constraint
     * @param coeffs map of variable name to coefficient in the constraint
     * @param rel type of constraint (LEQ, GEQ, EQ)
     * @param rhs right-hand side value of the constraint
     * @return the new Constraint object
     * @throws IllegalArgumentException if the name already exists
     * @throws IllegalStateException if the model is already built
     */
    public Constraint addConstraint(String name, Map<String, Double> coeffs, Constraint.Relation rel, double rhs) {
        checkNotBuilt();
        if (constraintNameToIndex.containsKey(name)) {
            throw new IllegalArgumentException("Constraint name already exists: " + name);
        }
        Constraint c = new Constraint(name, coeffs, rel, rhs);
        int idx = constraints.size();
        constraints.add(c);
        constraintNameToIndex.put(name, idx);
        return c;
    }

    /**
     * Sets the objective function for the model.
     *
     * @param coeffs map of variable name to objective coefficient
     * @param maximize true if maximization, false if minimization
     * @throws IllegalStateException if the model is already built
     */
    public void setObjective(Map<String, Double> coeffs, boolean maximize) {
        checkNotBuilt();
        objectiveCoefficients.clear();
        objectiveCoefficients.putAll(coeffs);
        this.maximize = maximize;
    }

    /**
     * Finalizes the model, assigns indices to variables and constraints.
     * After calling build(), no further variables or constraints can be added.
     * Must be called before passing the model to any solver backend.
     */
    public void build() {
        if (built) return;
        // Assign indices for fast lookup by solver backends
        for (int i = 0; i < variables.size(); i++) {
            variables.get(i).setIndex(i);
        }
        for (int i = 0; i < constraints.size(); i++) {
            constraints.get(i).setIndex(i);
        }
        built = true;
    }

    /**
     * @return an unmodifiable list of all variables
     */
    public List<Variable> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    /**
     * Returns an unmodifiable list of all constraints.
     */
    public List<Constraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    /**
     * Returns an unmodifiable map of the objective function coefficients.
     */
    public Map<String, Double> getObjectiveCoefficients() {
        return Collections.unmodifiableMap(objectiveCoefficients);
    }

    /**
     * @return true if the problem is maximization, false if minimization
     */
    public boolean isMaximize() {
        return maximize;
    }

    /**
     * @return true if build() has been called and the model is finalized
     */
    public boolean isBuilt() {
        return built;
    }

    /**
     * Returns a human-readable string of the model for debugging.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AbstractLPModel:\n");
        sb.append("Variables:\n");
        for (Variable v : variables) sb.append("  ").append(v).append("\n");
        sb.append("Constraints:\n");
        for (Constraint c : constraints) sb.append("  ").append(c).append("\n");
        sb.append("Objective: ").append(objectiveCoefficients)
                .append(" maximize=").append(maximize).append("\n");
        return sb.toString();
    }

    protected void doBuild() { /* nothing here for base class */ }
}
