package org.optsolvx.solver;

import org.optsolvx.model.AbstractLPModel;
import org.optsolvx.model.Constraint;
import org.optsolvx.model.OptimizationDirection;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Optimisation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ojAlgo backend for OptSolvX.
 * Builds an ExpressionsBasedModel with variables/bounds/linear constraints.
 * Equality -> level(rhs),  <= -> upper(rhs),  >= -> lower(rhs).
 * We always call minimise(); for MAX we flip objective weight to -1.
 * Objective value is recomputed from the returned variable values.
 */
public final class OjAlgoSolver implements LPSolverAdapter {

    @Override
    public LPSolution solve(AbstractLPModel model) {
        if (model == null) throw new IllegalArgumentException("Model must not be null.");
        if (!model.isBuilt()) model.build();

        final List<org.optsolvx.model.Variable> vars = model.getVariables();
        final int n = vars.size();

        // ----- ojAlgo model -----
        final ExpressionsBasedModel ebm = new ExpressionsBasedModel();
        final Map<String, org.ojalgo.optimisation.Variable> oj = new LinkedHashMap<>(n);

        // Variables + bounds
        for (org.optsolvx.model.Variable v : vars) {
            final org.ojalgo.optimisation.Variable ov = ebm.addVariable(v.getName());
            final double lb = v.getLowerBound();
            final double ub = v.getUpperBound();
            if (!Double.isInfinite(lb)) ov.lower(lb);
            if (!Double.isInfinite(ub)) ov.upper(ub);
            oj.put(v.getName(), ov);
        }

        // Linear constraints
        for (Constraint c : model.getConstraints()) {
            final Expression ex = ebm.addExpression(c.getName());
            for (Map.Entry<String, Double> term : c.getCoefficients().entrySet()) {
                final org.ojalgo.optimisation.Variable ov = oj.get(term.getKey());
                if (ov != null) ex.set(ov, term.getValue());
            }
            switch (c.getRelation()) {
                case LEQ:
                    ex.upper(c.getRhs());
                    break;
                case GEQ:
                    ex.lower(c.getRhs());
                    break;
                case EQ:
                    ex.level(c.getRhs());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown relation: " + c.getRelation());
            }
        }

        // Objective (weight flip for MAX)
        final Expression obj = ebm.addExpression("objective");
        for (Map.Entry<String, Double> e : model.getObjectiveCoefficients().entrySet()) {
            final org.ojalgo.optimisation.Variable ov = oj.get(e.getKey());
            if (ov != null) obj.set(ov, e.getValue());
        }
        final boolean maximise = model.getDirection() == OptimizationDirection.MAXIMIZE;
        obj.weight(maximise ? -1.0 : +1.0);

        // ----- Solve -----
        boolean feasible;
        Optimisation.Result result;
        try {
            result = ebm.minimise(); // weight handles MAX
            feasible = result.getState() != null && result.getState().isFeasible();
        } catch (Throwable t) {
            feasible = false;
            result = null;
        }

        // Values in declared order
        final Map<String, Double> values = new LinkedHashMap<>(n);
        for (org.optsolvx.model.Variable v : vars) {
            final org.ojalgo.optimisation.Variable ov = oj.get(v.getName());
            double val = 0.0;
            if (ov != null) {
                try {
                    final Number num = ov.getValue();
                    if (num != null) val = num.doubleValue();
                } catch (Throwable ignored) {
                }
            }
            values.put(v.getName(), val);
        }

        // Recompute objective (backend-independent)
        double objectiveValue = Double.NaN;
        if (feasible) {
            double sum = 0.0;
            for (Map.Entry<String, Double> e : model.getObjectiveCoefficients().entrySet()) {
                final Double x = values.get(e.getKey());
                if (x != null) sum += e.getValue() * x;
            }
            objectiveValue = sum;
        }

        return new LPSolution(values, objectiveValue, feasible);
    }
}