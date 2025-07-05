package org.optsolvx.solver;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Expression;
import org.apache.commons.math3.optim.linear.Relationship;
import java.util.*;

/**
 * Adapter implementation of ILPSolver using ojAlgo as backend.
 * Allows solving linear programming (LP) problems through a unified interface.
 */
public class OjAlgoSolver implements ILPSolver {

    // ojAlgo model object for building and solving LPs
    private ExpressionsBasedModel model;
    // List of all variables in the model (order is important)
    private List<Variable> variables;
    // List of all constraints added (for debugging purposes)
    private List<Expression> constraints;
    // Coefficients for the objective function
    private double[] objectiveCoeffs;
    // If true: maximize, if false: minimize
    private boolean maximize;

    /**
     * Initializes a new LP model with given number of variables.
     * Each variable is created with a default lower bound of 0.
     */
    @Override
    public void initializeModel(int numVars) {
        model = new ExpressionsBasedModel();
        variables = new ArrayList<>();
        constraints = new ArrayList<>();
        for (int i = 0; i < numVars; i++) {
            // Create variable with default lower bound 0 and add to the model
            Variable var = model.addVariable("x" + (i + 1)).lower(0); // Standard: >=0
            variables.add(var);
        }
    }

    /**
     * Sets the objective function.
     * @param coefficients Coefficient array for the variables (length must match variable count).
     * @param maximize If true, maximize; if false, minimize the objective.
     */
    @Override
    public void setObjective(double[] coefficients, boolean maximize) {
        this.objectiveCoeffs = coefficients;
        this.maximize = maximize;
    }

    /**
     * Adds a linear constraint to the model.
     * @param coefficients Coefficient array for the constraint (same order as variables).
     * @param relationship relationship Type of constraint (LEQ, GEQ, EQ).
     * @param value Right-hand side value for the constraint.
     */
    @Override
    public void addConstraint(double[] coefficients, Relationship relationship, double value) {
        Expression expr = model.addExpression("C" + (constraints.size() + 1));
        for (int i = 0; i < coefficients.length; i++) {
            expr.set(variables.get(i), coefficients[i]);
        }
        // Set the relationship (upper/lower/level)
        switch (relationship) {
            case LEQ:
                expr.upper(value);
                break;
            case GEQ:
                expr.lower(value);
                break;
            case EQ:
                expr.level(value);
                break;
        }
        constraints.add(expr);
    }

    /**
     * Sets lower and upper bounds for a variable by its index.
     * @param varIndex
     * @param lower
     * @param upper
     */
    @Override
    public void setVariableBounds(int varIndex, double lower, double upper) {
        variables.get(varIndex).lower(lower);
        variables.get(varIndex).upper(upper);
    }

    /**
     * Solves the LP and return the solution as LPSolution.
     * @return LPSolution object with variable values, objective and feasibility flag.
     */
    @Override
    public LPSolution solve() {
        // Add objective expression to the model
        Expression objective = model.addExpression("OBJ");
        for (int i = 0; i < objectiveCoeffs.length; i++) {
            objective.set(variables.get(i), objectiveCoeffs[i]);
        }
        // Weight 1.0 for maximization; ojAlgo distinguishes via the solve call
        objective.weight(1.0);

        // Solve the model
        Optimisation.Result result = maximize ? model.maximise() : model.minimise();

        // Check feasibility
        boolean feasible = result.getState().isFeasible() || result.getState().isOptimal();
        double[] solution = new double[variables.size()];

        // Fill the solution-array, index based in variable sequence
        for (int i = 0; i < variables.size(); i++) {
            solution[i] = result.get(i).doubleValue();
        }

        // Return the solution
        return new LPSolution(solution, result.getValue(), feasible);
    }

    /**
     * Resets the solver by clearing the model and all parameters.
     */
    @Override
    public void reset() {
        model = null;
        variables = null;
        constraints = null;
        objectiveCoeffs = null;
    }
}
