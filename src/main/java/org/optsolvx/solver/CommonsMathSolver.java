package org.optsolvx.solver;

import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.MaxIter;
import java.util.*;

public class CommonsMathSolver implements ILPSolver {
    // Interne Felder...
    private int numVars;
    private double[] objectiveCoeffs;
    private boolean maximize;
    private List<LinearConstraint> constraints = new ArrayList<>();
    private double[] lowerBounds;
    private double[] upperBounds;

    @Override
    public void initializeModel(int numVars) {
        this.numVars = numVars;
        this.lowerBounds = new double[numVars];
        this.upperBounds = new double[numVars];
        Arrays.fill(this.lowerBounds, 0);
        Arrays.fill(this.upperBounds, Double.POSITIVE_INFINITY);
    }

    @Override
    public void setObjective(double[] coefficients, boolean maximize) {
        this.objectiveCoeffs = coefficients;
        this.maximize = maximize;
    }

    @Override
    public void addConstraint(double[] coefficients, Relationship relationship, double value) {
        constraints.add(new LinearConstraint(coefficients, relationship, value));
    }

    @Override
    public void setVariableBounds(int varIndex, double lower, double upper) {
        this.lowerBounds[varIndex] = lower;
        this.upperBounds[varIndex] = upper;
    }

    @Override
    public LPSolution solve() {
        // SimplexSolver verwenden, wie im Prototyp.
        // TODO: Variable bounds einbauen!
        LinearObjectiveFunction objective = new LinearObjectiveFunction(objectiveCoeffs, 0);
        SimplexSolver solver = new SimplexSolver();
        try {
            PointValuePair result = solver.optimize(
                    new MaxIter(100),
                    objective,
                    new LinearConstraintSet(constraints),
                    maximize ? GoalType.MAXIMIZE : GoalType.MINIMIZE,
                    new NonNegativeConstraint(true)
            );
            double[] variables = result.getPoint();
            double obj = result.getValue();
            return new LPSolution(variables, obj, true);
        } catch (Exception e) {
            return new LPSolution(new double[numVars], Double.NaN, false);
        }
    }

    @Override
    public void reset() {
        constraints.clear();
    }
}
