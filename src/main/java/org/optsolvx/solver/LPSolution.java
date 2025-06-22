package org.optsolvx.solver;

public class LPSolution {
    public final double[] variableValues;
    public final double objectiveValue;
    public final boolean feasible;

    public LPSolution(double[] variableValues, double objectiveValue, boolean feasible) {
        this.variableValues = variableValues;
        this.objectiveValue = objectiveValue;
        this.feasible = feasible;
    }
}
