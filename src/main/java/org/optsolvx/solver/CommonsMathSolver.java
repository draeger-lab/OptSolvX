package org.optsolvx.solver;

import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.MaxIter;
import java.util.*;

/**
 * Adapter Implementation of ILPSolver using Apache Commons Math as backend.
 * Encapsulates the SimplexSolver for linear programs (LP), mapping OptSolvX API
 * calls to the Common Math solver structures. This implementation covers
 * standard LPs but has some limitations in variable bounds (see below).
 */
public class CommonsMathSolver implements ILPSolver {

    // Number of decision variables in the model
    private int numVars;
    // Objective function coefficients (size = numVars)
    private double[] objectiveCoeffs;
    // Flag indicating maximize (true) or minimize (false)
    private boolean maximize;
    // List of all linear constraints in the model
    private List<LinearConstraint> constraints = new ArrayList<>();
    // Per-variable lower and upper bounds (size = numVars)
    private double[] lowerBounds;
    private double[] upperBounds;

    /**
     * Initializes the solver for a new problem instance.
     * Resets constraints and sets default bounds (lower = 0, upper = +Inf).
     * @param numVars Number of variables for the problem
     */
    @Override
    public void initializeModel(int numVars) {
        this.numVars = numVars;
        this.lowerBounds = new double[numVars];
        this.upperBounds = new double[numVars];
        Arrays.fill(this.lowerBounds, 0); // Standard LP: all variables >= 0
        Arrays.fill(this.upperBounds, Double.POSITIVE_INFINITY); // No explicit upper bound
    }

    /**
     * Defines the objective function to the optimized
     * @param coefficients Coefficient array, same order as variables
     * @param maximize True or maximization, false for minimization
     */
    @Override
    public void setObjective(double[] coefficients, boolean maximize) {
        this.objectiveCoeffs = coefficients;
        this.maximize = maximize;
    }

    /**
     * Adds a linear constraint to the model.
     * @param coefficients Coefficient array for the constraint (length = numVars)
     * @param relationship Relationship of the constraint (LEQ, GEQ, EQ)
     * @param value Right-hand side value for the constraint
     */
    @Override
    public void addConstraint(double[] coefficients, Relationship relationship, double value) {
        constraints.add(new LinearConstraint(coefficients, relationship, value));
    }

    /**
     * Sets explicit lower and upper bounds for a variable.
     * NOTE: Commons Math SimplexSolver only natively supports non-negativity;
     * for general bounds, these would need to be transformed into additional constraints.
     * @param varIndex Index of the variable (0-based)
     * @param lower Lower bound
     * @param upper Upper bound
     */
    @Override
    public void setVariableBounds(int varIndex, double lower, double upper) {
        this.lowerBounds[varIndex] = lower;
        this.upperBounds[varIndex] = upper;
    }

    /**
     * Solves the current LP using Commons Math SimplexSolver.
     * Variable bounds beyond non-negativity are NOT enforced unless added as constraints.
     * Returns infeasible solution if any solver error occurs.
     * @return LPSolution (variables, objective, feasible flag)
     */
    @Override
    public LPSolution solve() {
        // TODO: Variable bounds einbauen!
        // Build the objective function
        LinearObjectiveFunction objective = new LinearObjectiveFunction(objectiveCoeffs, 0);

        // NoteL Only lower bounds of zero are supported directly via NonNegativeConstraint
        // If general variable bounds are set, user must add them as explicit constraints
        SimplexSolver solver = new SimplexSolver();
        try {
            PointValuePair result = solver.optimize(
                    new MaxIter(100),
                    objective,
                    new LinearConstraintSet(constraints),
                    maximize ? GoalType.MAXIMIZE : GoalType.MINIMIZE,
                    new NonNegativeConstraint(true)
            );
            // Retrieve variable values (order matches model variables)
            double[] variables = result.getPoint();
            double obj = result.getValue();
            return new LPSolution(variables, obj, true);
        } catch (Exception e) {
            // Any exception (infeasible, unbounded, numerical, etc.) is mapped to an infeasible solution
            return new LPSolution(new double[numVars], Double.NaN, false);
        }
    }

    /**
     * Resets all model data for reuse of the solver instance.
     * Does not change number of variables or bounds.
     */
    @Override
    public void reset() {
        constraints.clear();
    }
}
