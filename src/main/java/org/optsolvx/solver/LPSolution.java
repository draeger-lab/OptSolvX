package org.optsolvx.solver;

import java.util.Map;

public class LPSolution {

    private final Map<String, Double> variableValues; // Variable name -> value
    private final double objectiveValue;
    private final boolean feasible; // true if solution is feasible

    public LPSolution(Map<String, Double> variableValues, double objectValue, boolean feasible) {
        this.variableValues = variableValues;
        this.objectiveValue = objectValue;
        this.feasible = feasible;
    }

    public Map<String, Double> getVariableValues() {
        return variableValues;
    }

    public double getObjectiveValue() {
        return objectiveValue;
    }

    public boolean isFeasible() {
        return feasible;
    }
}
