package org.optsolvx.model;

import org.apache.commons.math3.optim.linear.Relationship;
import java.util.*;

/**
 * Generic base model for linear programs. Stores all variables, constraints, and objective.
 */
public class AbstractLPModel {

    private final List<Variable> variables = new ArrayList<>();
    private final List<Constraint> constraints = new ArrayList<>();
    private final Map<String, Integer> varNameToIndex = new HashMap<>();
    private final Map<String, Integer> constraintNameToIndex = new HashMap<>();
    private final Map<String, Double> objectiveCoefficients = new HashMap<>();
    private boolean maximize = true;
    private boolean built = false;

    public int addVariable(String name, double lower, double upper) {
        if (built) throw new IllegalStateException("Model already built.");
        if (varNameToIndex.containsKey(name))
            throw new IllegalArgumentException("Variable name already exists: " + name);
        Variable var = new Variable(name, lower, upper);
        int idx = variables.size();
        variables.add(var);
        varNameToIndex.put(name, idx);
        return idx;
    }

    public int addConstraint(String name, Map<String, Double> coeffs, Relationship rel, double rhs) {
        if (built) throw new IllegalStateException("Model already built.");
        if (constraintNameToIndex.containsKey(name))
            throw new IllegalArgumentException("Constraint name already exists: " + name);
        Constraint c = new Constraint(name, coeffs, rel, rhs);
        int idx = constraints.size();
        constraints.add(c);
        constraintNameToIndex.put(name, idx);
        return idx;
    }

    public void setObjective(Map<String, Double> coeffs, boolean maximize) {
        if (built) throw new IllegalStateException("Model already built.");
        objectiveCoefficients.clear();
        objectiveCoefficients.putAll(coeffs);
        this.maximize = maximize;
    }

    /**
     * "Fixes" the model and assigns indices to all variables and constraints.
     * After this, no further variables/constraints can be added.
     */
    public void build() {
        if (built) return;
        for (int i = 0; i < variables.size(); i++)
            variables.get(i).setIndex(i);
        for (int i = 0; i < constraints.size(); i++)
            constraints.get(i).setIndex(i);
        built = true;
    }

    public Variable getVariable(String name) {
        Integer idx = varNameToIndex.get(name);
        if (idx == null) throw new IllegalArgumentException("No such variable: " + name);
        return variables.get(idx);
    }

    public Constraint getConstraint(String name) {
        Integer idx = constraintNameToIndex.get(name);
        if (idx == null) throw new IllegalArgumentException("No such constraint: " + name);
        return constraints.get(idx);
    }

    public List<Variable> getVariables() { return Collections.unmodifiableList(variables); }
    public List<Constraint> getConstraints() { return Collections.unmodifiableList(constraints); }
    public Map<String, Double> getObjectiveCoefficients() { return Collections.unmodifiableMap(objectiveCoefficients); }
    public boolean isMaximize() { return maximize; }
    public boolean isBuilt() { return built; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AbstractLPModel:\n");
        sb.append("Variables:\n");
        for (Variable v : variables) sb.append("  ").append(v).append("\n");
        sb.append("Constraints:\n");
        for (Constraint c : constraints) sb.append("  ").append(c).append("\n");
        sb.append("Objective: ").append(objectiveCoefficients).append(" maximize=").append(maximize).append("\n");
        return sb.toString();
    }
}
