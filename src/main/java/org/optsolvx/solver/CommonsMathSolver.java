package org.optsolvx.solver;

import org.optsolvx.model.AbstractLPModel;
import org.optsolvx.model.Variable;
import org.optsolvx.model.Constraint;
import org.optsolvx.model.Constraint.Relation;
import org.optsolvx.model.OptimizationDirection;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optim.OptimizationData;

import java.util.*;

/**
 * Commons Math 3 backend for OptSolvX.
 * Implementation notes:
 * - Equality constraints are represented as two inequalities (<= and >=).
 * - Variable bounds are added explicitly as linear constraints (lb/ub).
 * - NonNegativeConstraint(false) is required to allow negative fluxes.
 * - If the problem is infeasible or unbounded, the solution is marked infeasible
 * and the objective is reported as NaN to mirror legacy behavior.
 */
public final class CommonsMathSolver implements LPSolverAdapter {

    private static final int MAX_ITERS = 10_000;

    @Override
    public LPSolution solve(AbstractLPModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Model must not be null.");
        }
        // Ensure model is frozen before solving.
        if (!model.isBuilt()) {
            model.build();
        }

        final List<Variable> vars = model.getVariables();
        final int n = vars.size();

        // ----- Objective -----
        final double[] objective = new double[n];
        for (Map.Entry<String, Double> e : model.getObjectiveCoefficients().entrySet()) {
            final int idx = model.getVariableIndex(e.getKey());
            objective[idx] = e.getValue();
        }
        final LinearObjectiveFunction f = new LinearObjectiveFunction(objective, 0.0);
        final GoalType goal = (model.getDirection() == OptimizationDirection.MAXIMIZE)
                ? GoalType.MAXIMIZE : GoalType.MINIMIZE;

        // ----- Constraints from model -----
        final Collection<LinearConstraint> cons = new ArrayList<>();

        // Linear constraints (EQ → two inequalities)
        for (Constraint c : model.getConstraints()) {
            final double[] a = new double[n];
            for (Map.Entry<String, Double> term : c.getCoefficients().entrySet()) {
                a[model.getVariableIndex(term.getKey())] = term.getValue();
            }
            final double rhs = c.getRhs();
            final Relation rel = c.getRelation();
            switch (rel) {
                case LEQ:
                    cons.add(new LinearConstraint(a, Relationship.LEQ, rhs));
                    break;
                case GEQ:
                    cons.add(new LinearConstraint(a, Relationship.GEQ, rhs));
                    break;
                case EQ:
                    cons.add(new LinearConstraint(a, Relationship.LEQ, rhs));
                    cons.add(new LinearConstraint(a, Relationship.GEQ, rhs));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown relation: " + rel);
            }
        }

        // Variable bounds (as constraints)
        for (int i = 0; i < n; i++) {
            final Variable v = vars.get(i);
            final double lb = v.getLowerBound();
            final double ub = v.getUpperBound();

            // x_i >= lb
            if (!Double.isInfinite(lb)) {
                final double[] a = new double[n];
                a[i] = 1.0;
                cons.add(new LinearConstraint(a, Relationship.GEQ, lb));
            }
            // x_i <= ub
            if (!Double.isInfinite(ub)) {
                final double[] a = new double[n];
                a[i] = 1.0;
                cons.add(new LinearConstraint(a, Relationship.LEQ, ub));
            }
        }

        // ----- Optimize -----
        final SimplexSolver solver = new SimplexSolver();
        PointValuePair pvp;
        boolean feasible = true;
        double objectiveValue = Double.NaN;
        double[] point = new double[n];

        try {
            pvp = solver.optimize(
                    new MaxIter(MAX_ITERS),
                    f,
                    new LinearConstraintSet(cons),
                    goal,
                    // IMPORTANT: allow negative values (fluxes can be negative)
                    new NonNegativeConstraint(false)
            );
            if (pvp == null || pvp.getPoint() == null) {
                feasible = false;
            } else {
                point = pvp.getPoint();
                // Guard: some solvers can return shorter arrays in degenerate cases
                if (point.length < n) {
                    point = Arrays.copyOf(point, n);
                }
                objectiveValue = pvp.getValue();
            }
        } catch (Exception ex) {
            // Infeasible/unbounded/iteration limit → mark infeasible, keep NaN objective
            feasible = false;
        }

        // Build name → value map in declared variable order
        final Map<String, Double> values = new LinkedHashMap<String, Double>(n);
        for (int i = 0; i < n; i++) {
            final String name = vars.get(i).getName();
            final double val = (i < point.length) ? point[i] : 0.0;
            values.put(name, val);
        }

        return new LPSolution(values, objectiveValue, feasible);
    }
}
