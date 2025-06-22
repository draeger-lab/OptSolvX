package org.optsolvx.solver;

import org.apache.commons.math3.optim.linear.Relationship;

public interface ILPSolver {
    void initializeModel(int numVars); // Reserviert Platz für Variablen
    void setObjective(double[] coefficients, boolean maximize);
    void addConstraint(double[] coefficients, Relationship relationship, double value);
    void setVariableBounds(int varIndex, double lower, double upper);
    LPSolution solve();
    void reset();
}
