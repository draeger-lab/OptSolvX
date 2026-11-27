
package org.optsolvx.backend.commonsmath;

import org.optsolvx.model.AbstractLPModel;
import org.optsolvx.model.Variable;
import org.optsolvx.model.Constraint;
import org.optsolvx.model.OptimizationDirection;
import org.optsolvx.solver.LPSolution;
import org.optsolvx.solver.LPSolverAdapter;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommonsMathSolver implements LPSolverAdapter {

    private static final int MAX_ITERS = 50_000;
    private static final double TOL = 1e-6;

    @Override
    public LPSolution solve(AbstractLPModel model) {

        // Preserve the existing input validation.
        if (model == null || !model.isBuilt()) {
            return solveAttempt(model, false);
        }

        // Prefer a smaller tableau for large LPs.
        final boolean lowerShiftFirst =
                model.getVariables().size() >= 8192;

        final LPSolution first =
                solveAttempt(model, lowerShiftFirst);

        if (first.isFeasible()) {
            return first;
        }

        System.out.println(
                "LP_STRATEGY_RETRY: "
                        + (lowerShiftFirst
                            ? "CENTERED"
                            : "LOWER_SHIFT"));

        return solveAttempt(model, !lowerShiftFirst);
    }


    private LPSolution solveAttempt(
            final AbstractLPModel model,
            final boolean lowerShift) {

        System.out.println(
                "LP_STRATEGY: "
                        + (lowerShift
                            ? "LOWER_SHIFT"
                            : "CENTERED"));
        if (model == null) {
            throw new IllegalArgumentException("Model must not be null.");
        }

        if (!model.isBuilt()) {
            throw new IllegalStateException("Model must be built.");
        }

        final List<Variable> vars = model.getVariables();
        final int n = vars.size();

        final int[] root = new int[n];
        final int[] sign = new int[n];
        final double[] lower = new double[n];
        final double[] upper = new double[n];

        for (int i = 0; i < n; i++) {
            root[i] = i;
            sign[i] = 1;
            lower[i] = vars.get(i).getLowerBound();
            upper[i] = vars.get(i).getUpperBound();

            if (Double.isNaN(lower[i])
                    || Double.isNaN(upper[i])
                    || lower[i] == Double.POSITIVE_INFINITY
                    || upper[i] == Double.NEGATIVE_INFINITY
                    || lower[i] > upper[i]) {
                throw new IllegalArgumentException(
                        "Invalid variable bounds.");
            }
        }

        int zeroFixes = 0;
        int mergedVariables = 0;
        int passes = 0;

        boolean changed;

        // Propagate zero fluxes and signed variable equivalences.
        do {
            changed = false;
            passes++;

            for (Constraint c : model.getConstraints()) {

                if (c.getRelation() != Constraint.Relation.EQ
                        || c.getRhs() != 0.0d) {
                    continue;
                }

                final Map<Integer, BigDecimal> terms =
                        aggregate(model, c, root, sign);

                terms.entrySet().removeIf(e ->
                        lower[e.getKey()] == 0.0d
                        && upper[e.getKey()] == 0.0d);

                if (terms.isEmpty()) {
                    continue;
                }

                // A homogeneous singleton forces zero.
                if (terms.size() == 1) {
                    int r = terms.keySet().iterator().next();

                    if (lower[r] > 0.0d || upper[r] < 0.0d) {
                        return failedSolution(vars);
                    }

                    lower[r] = 0.0d;
                    upper[r] = 0.0d;

                    zeroFixes++;
                    changed = true;
                    continue;
                }

                // Detect rows whose terms all have the same sign.
                boolean nonNegative = true;
                boolean nonPositive = true;

                for (Map.Entry<Integer, BigDecimal> e
                        : terms.entrySet()) {

                    final int r = e.getKey();
                    final boolean positive =
                            e.getValue().signum() > 0;

                    nonNegative &= positive
                            ? lower[r] >= 0.0d
                            : upper[r] <= 0.0d;

                    nonPositive &= positive
                            ? upper[r] <= 0.0d
                            : lower[r] >= 0.0d;
                }

                if (nonNegative || nonPositive) {
                    for (int r : terms.keySet()) {

                        if (lower[r] > 0.0d || upper[r] < 0.0d) {
                            return failedSolution(vars);
                        }

                        lower[r] = 0.0d;
                        upper[r] = 0.0d;
                        zeroFixes++;
                    }

                    changed = true;
                    continue;
                }

                // Merge variables linked by x = y or x = -y.
                if (terms.size() == 2) {
                    List<Map.Entry<Integer, BigDecimal>> pair =
                            new ArrayList<>(terms.entrySet());

                    final int a = pair.get(0).getKey();
                    final int b = pair.get(1).getKey();

                    final BigDecimal ca = pair.get(0).getValue();
                    final BigDecimal cb = pair.get(1).getValue();

                    if (ca.abs().compareTo(cb.abs()) != 0) {
                        continue;
                    }

                    final int factor =
                            -ca.signum() * cb.signum();

                    final double lb = factor == 1
                            ? lower[a] : -upper[a];

                    final double ub = factor == 1
                            ? upper[a] : -lower[a];

                    lower[b] = Math.max(lower[b], lb);
                    upper[b] = Math.min(upper[b], ub);

                    if (lower[b] > upper[b]) {
                        return failedSolution(vars);
                    }

                    for (int i = 0; i < n; i++) {
                        if (root[i] == a) {
                            root[i] = b;
                            sign[i] *= factor;
                        }
                    }

                    mergedVariables++;
                    changed = true;
                }
            }

        } while (changed);


        final int[] index = new int[n];
        final int[] negativeIndex = new int[n];
        final int[] orientation = new int[n];
        final double[] offsets = new double[n];

        Arrays.fill(index, -1);
        Arrays.fill(negativeIndex, -1);
        Arrays.fill(orientation, 1);

        final List<Double> widths = new ArrayList<>();

        int fixedRoots = 0;
        int oneSidedRoots = 0;
        int freeRoots = 0;

        for (int i = 0; i < n; i++) {

            if (root[i] != i) {
                continue;
            }

            final double lb = lower[i];
            final double ub = upper[i];

            if (lb == ub) {
                offsets[i] = lb;
                fixedRoots++;
                continue;
            }

            if (Double.isFinite(lb)
                    && Double.isFinite(ub)) {

                if (lowerShift) {

                    // Preserve the validated finite-bound transformation.
                    offsets[i] = lb;

                    index[i] = widths.size();
                    final double width = ub - lb;

                    if (!Double.isFinite(width)) {
                        return failedSolution(vars);
                    }

                    widths.add(width);

                } else {

                    // Prefer the origin whenever it is feasible.
                    if (lb < 0.0d && ub > 0.0d) {

                        offsets[i] = 0.0d;

                        index[i] = widths.size();
                        widths.add(ub);

                        negativeIndex[i] = widths.size();
                        widths.add(-lb);

                    } else {

                        // Use the finite bound closest to zero.
                        final double width = ub - lb;

                        if (!Double.isFinite(width)) {
                            return failedSolution(vars);
                        }

                        if (ub <= 0.0d) {
                            offsets[i] = ub;
                            orientation[i] = -1;
                        } else {
                            offsets[i] = lb;
                        }

                        index[i] = widths.size();
                        widths.add(width);
                    }

                }

            } else if (Double.isFinite(lb)) {

                // Lower-bounded variable.
                offsets[i] = lb;

                index[i] = widths.size();
                widths.add(Double.POSITIVE_INFINITY);

                oneSidedRoots++;

            } else if (Double.isFinite(ub)) {

                // Upper-bounded variable.
                offsets[i] = ub;
                orientation[i] = -1;

                index[i] = widths.size();
                widths.add(Double.POSITIVE_INFINITY);

                oneSidedRoots++;

            } else {

                // Free variable: x = positive - negative.
                offsets[i] = 0.0d;

                index[i] = widths.size();
                widths.add(Double.POSITIVE_INFINITY);

                negativeIndex[i] = widths.size();
                widths.add(Double.POSITIVE_INFINITY);

                freeRoots++;
            }
        }

        final int m = widths.size();

        System.out.println("One-sided unbounded roots: "
                + oneSidedRoots);

        System.out.println("Free roots: " + freeRoots);

        System.out.println("Presolve passes: " + passes);
        System.out.println("Original variables: " + n);
        System.out.println("Derived zero fixes: " + zeroFixes);
        System.out.println("Merged variables: " + mergedVariables);
        System.out.println("Fixed root variables: " + fixedRoots);
        System.out.println("Reduced variables: " + m);



        final double[] columnScale = new double[m];

        double minimumScale = Double.POSITIVE_INFINITY;
        double maximumScale = 0.0d;

        for (int j = 0; j < m; j++) {

            final double width = widths.get(j);

            if (Double.isNaN(width)
                    || width <= 0.0d) {
                return failedSolution(vars);
            }

            final double scale =
                    Double.isInfinite(width)
                            ? 1.0d
                            : Math.scalb(
                                    1.0d,
                                    Math.getExponent(width));

            if (!Double.isFinite(scale)
                    || scale <= 0.0d) {
                return failedSolution(vars);
            }

            columnScale[j] = scale;

            minimumScale = Math.min(minimumScale, scale);
            maximumScale = Math.max(maximumScale, scale);
        }

        System.out.println("Minimum column scale: "
                + minimumScale);

        System.out.println("Maximum column scale: "
                + maximumScale);

        final double[] objective = new double[m];



        // Transform objective coefficients.
        for (Map.Entry<String, Double> e
                : model.getObjectiveCoefficients().entrySet()) {

            final int i = model.getVariableIndex(e.getKey());
            final int r = root[i];

            if (index[r] >= 0) {

                objective[index[r]] +=
                        e.getValue() * sign[i]
                                * orientation[r]
                                * columnScale[index[r]];
            }

            if (negativeIndex[r] >= 0) {

                objective[negativeIndex[r]] -=
                        e.getValue() * sign[i]
                                * columnScale[negativeIndex[r]];
            }
        }

        final List<LinearConstraint> constraints =
                new ArrayList<>();

        int removedRows = 0;

        // Substitute aliases and shift remaining coordinates.
        for (Constraint c : model.getConstraints()) {
            final double[] coefficients = new double[m];

            BigDecimal rhs = new BigDecimal(c.getRhs());

            for (Map.Entry<Integer, BigDecimal> e
                    : aggregate(model, c, root, sign).entrySet()) {

                final int r = e.getKey();

                rhs = rhs.subtract(e.getValue().multiply(
                        new BigDecimal(offsets[r])));

                if (index[r] >= 0) {
                    final double coefficient =
                            e.getValue().doubleValue()
                                    * orientation[r];

                    final double scaledCoefficient =
                            coefficient * columnScale[index[r]];

                    if (!Double.isFinite(scaledCoefficient)
                            || (coefficient != 0.0d
                            && scaledCoefficient == 0.0d)) {
                        return failedSolution(vars);
                    }

                    coefficients[index[r]] =
                            scaledCoefficient;

                    if (negativeIndex[r] >= 0) {

                        coefficients[negativeIndex[r]] =
                                -e.getValue().doubleValue()
                                * columnScale[negativeIndex[r]];
                    }
                }
            }

            final Relationship relationship;

            switch (c.getRelation()) {
                case EQ:
                    relationship = Relationship.EQ;
                    break;
                case LEQ:
                    relationship = Relationship.LEQ;
                    break;
                case GEQ:
                    relationship = Relationship.GEQ;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown relationship.");
            }

            boolean constant = true;

            for (double coefficient : coefficients) {
                if (coefficient != 0.0d) {
                    constant = false;
                    break;
                }
            }

            if (constant) {
                final boolean satisfied;

                switch (relationship) {
                    case EQ:
                        satisfied = rhs.signum() == 0;
                        break;
                    case LEQ:
                        satisfied = rhs.signum() >= 0;
                        break;
                    case GEQ:
                        satisfied = rhs.signum() <= 0;
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown relationship.");
                }

                if (!satisfied) {
                    return failedSolution(vars);
                }

                removedRows++;
                continue;
            }

            if (!Double.isFinite(rhs.doubleValue())) {
                return failedSolution(vars);
            }


            // Normalize the row using a power-of-two factor.
            double maximum = Math.abs(rhs.doubleValue());

            for (double coefficient : coefficients) {
                maximum = Math.max(
                        maximum,
                        Math.abs(coefficient));
            }

            if (!Double.isFinite(maximum)
                    || maximum == 0.0d) {
                return failedSolution(vars);
            }

            final double factor = Math.scalb(
                    1.0d, -Math.getExponent(maximum));

            for (int j = 0; j < m; j++) {
                final double previous = coefficients[j];
                coefficients[j] *= factor;

                if (!Double.isFinite(coefficients[j])
                        || (previous != 0.0d
                        && coefficients[j] == 0.0d)) {
                    return failedSolution(vars);
                }
            }

            final double scaledRhs =
                    rhs.doubleValue() * factor;

            if (!Double.isFinite(scaledRhs)
                    || (rhs.signum() != 0
                    && scaledRhs == 0.0d)) {
                return failedSolution(vars);
            }

            constraints.add(new LinearConstraint(
                    coefficients,
                    relationship,
                    scaledRhs));

        }

        System.out.println("Original model constraints: "
                + model.getConstraints().size());

        System.out.println("Removed model constraints: "
                + removedRows);

        System.out.println("Reduced model constraints: "
                + constraints.size());


        // Add finite upper bounds for transformed coordinates.
        for (int j = 0; j < m; j++) {

            if (widths.get(j) == Double.POSITIVE_INFINITY) {
                continue;
            }

            final double width =
                    widths.get(j) / columnScale[j];

            if (!Double.isFinite(width)
                    || width < 0.0d) {
                return failedSolution(vars);
            }

            final double[] coefficients = new double[m];
            coefficients[j] = 1.0d;

            constraints.add(new LinearConstraint(
                    coefficients,
                    Relationship.LEQ,
                    width));
        }


        // Export the exact transformed LP for independent validation.
        final String exportPath = System.getenv("LP_EXPORT");

        if (exportPath != null && m > 0) {

            try (java.io.PrintWriter out = new java.io.PrintWriter(
                    java.nio.file.Files.newBufferedWriter(
                            java.nio.file.Paths.get(exportPath)))) {

                java.util.function.Consumer<double[]> writeTerms =
                        coefficients -> {

                    int count = 0;

                    for (int j = 0; j < coefficients.length; j++) {

                        final double value = coefficients[j];

                        if (value == 0.0d) {
                            continue;
                        }

                        if (count > 0 && count % 8 == 0) {
                            out.println();
                        }

                        out.print(value < 0.0d ? " - " : " + ");
                        out.print(Math.abs(value));
                        out.print(" x" + j);

                        count++;
                    }

                    if (count == 0) {
                        out.print(" + 0 x0");
                    }
                };

                out.println(
                        model.getDirection().name().equals("MAXIMIZE")
                                ? "Maximize" : "Minimize");

                out.print(" obj:");
                writeTerms.accept(objective);
                out.println();

                out.println("Subject To");

                int rowNumber = 0;

                for (LinearConstraint constraint : constraints) {

                    out.print(" c" + rowNumber++ + ":");

                    writeTerms.accept(
                            constraint.getCoefficients().toArray());

                    switch (constraint.getRelationship()) {
                        case EQ:
                            out.print(" = ");
                            break;
                        case LEQ:
                            out.print(" <= ");
                            break;
                        case GEQ:
                            out.print(" >= ");
                            break;
                        default:
                            throw new IllegalStateException(
                                    "Unknown relationship.");
                    }

                    out.println(constraint.getValue());
                }

                out.println("Bounds");

                for (int j = 0; j < m; j++) {
                    out.println(" 0 <= x" + j);
                }

                out.println("End");

                if (out.checkError()) {
                    throw new java.io.IOException("LP export failed.");
                }

                // Preserve the objective constant introduced by shifting.
                double objectiveOffset = 0.0d;

                for (Map.Entry<String, Double> e
                        : model.getObjectiveCoefficients().entrySet()) {

                    final int i =
                            model.getVariableIndex(e.getKey());

                    objectiveOffset += e.getValue()
                            * sign[i] * offsets[root[i]];
                }

                java.nio.file.Files.write(
                        java.nio.file.Paths.get(exportPath + ".offset"),
                        java.util.Collections.singletonList(
                                Double.toString(objectiveOffset)));

                System.out.println("Exported transformed LP: "
                        + exportPath);

                System.out.println("Objective offset: "
                        + objectiveOffset);

            } catch (java.io.IOException ex) {
                throw new java.io.UncheckedIOException(ex);
            }
        }

        System.out.println("Total solver constraints: "
                + constraints.size());

        final GoalType goal =
                model.getDirection() == OptimizationDirection.MAXIMIZE
                        ? GoalType.MAXIMIZE
                        : GoalType.MINIMIZE;


        final SimplexSolver solver = new DiagnosticSimplexSolver()
                .withLowerShiftStrategy(lowerShift);

        System.out.println(
                "Using power-of-two row and column scaling");


        try {
            double[] point = new double[m];

            if (m > 0) {
                final PointValuePair result = solver.optimize(
                        new MaxIter(MAX_ITERS),
                        new LinearObjectiveFunction(
                                normalizeObjective(objective), 0.0d),
                        new LinearConstraintSet(constraints),
                        goal,
                        new NonNegativeConstraint(true)
                );

                if (result == null
                        || result.getPoint() == null
                        || result.getPoint().length != m) {
                    return failedSolution(vars);
                }

                point = result.getPoint();
            }

            final Map<String, Double> values =
                    new LinkedHashMap<>();

            double boundViolation = 0.0d;
            double constraintViolation = 0.0d;
            double objectiveValue = 0.0d;

            // Reconstruct original variables.
            for (int i = 0; i < n; i++) {
                final int r = root[i];


                double coordinate = offsets[r];

                if (index[r] >= 0) {

                    coordinate += orientation[r]
                            * point[index[r]]
                            * columnScale[index[r]];
                }

                if (negativeIndex[r] >= 0) {

                    coordinate -= point[negativeIndex[r]]
                            * columnScale[negativeIndex[r]];
                }

                final double value = sign[i] * coordinate;

                if (!Double.isFinite(value)) {
                    return failedSolution(vars);
                }

                values.put(vars.get(i).getName(), value);

                boundViolation = Math.max(
                        boundViolation,
                        vars.get(i).getLowerBound() - value);

                boundViolation = Math.max(
                        boundViolation,
                        value - vars.get(i).getUpperBound());
            }

            for (Map.Entry<String, Double> e
                    : model.getObjectiveCoefficients().entrySet()) {

                objectiveValue += e.getValue()
                        * values.get(e.getKey());
            }

            // Validate constraints in the original coordinates.
            for (Constraint c : model.getConstraints()) {
                double lhs = 0.0d;

                for (Map.Entry<String, Double> e
                        : c.getCoefficients().entrySet()) {

                    lhs += e.getValue()
                            * values.get(e.getKey());
                }

                final double residual = lhs - c.getRhs();

                switch (c.getRelation()) {
                    case EQ:
                        constraintViolation = Math.max(
                                constraintViolation,
                                Math.abs(residual));
                        break;
                    case LEQ:
                        constraintViolation = Math.max(
                                constraintViolation, residual);
                        break;
                    case GEQ:
                        constraintViolation = Math.max(
                                constraintViolation, -residual);
                        break;
                }
            }

            System.out.println("Iterations: "
                    + solver.getIterations());

            System.out.println("Objective: " + objectiveValue);

            System.out.println("Maximum bound violation: "
                    + boundViolation);

            System.out.println("Maximum constraint violation: "
                    + constraintViolation);

            if (!Double.isFinite(objectiveValue)
                    || !Double.isFinite(boundViolation)
                    || !Double.isFinite(constraintViolation)
                    || boundViolation > TOL
                    || constraintViolation > TOL) {
                return failedSolution(vars);
            }

            return new LPSolution(
                    values, objectiveValue, true);

        } catch (Exception ex) {
            System.err.println("Iterations: "
                    + solver.getIterations());

            ex.printStackTrace(System.err);

            return failedSolution(vars);
        }
    }

    // Aggregate signed aliases using exact coefficient arithmetic.
    private static Map<Integer, BigDecimal> aggregate(
            AbstractLPModel model,
            Constraint constraint,
            int[] root,
            int[] sign) {

        final Map<Integer, BigDecimal> terms =
                new LinkedHashMap<>();

        for (Map.Entry<String, Double> e
                : constraint.getCoefficients().entrySet()) {

            final int i = model.getVariableIndex(e.getKey());

            BigDecimal coefficient =
                    new BigDecimal(e.getValue());

            if (sign[i] < 0) {
                coefficient = coefficient.negate();
            }

            terms.merge(
                    root[i],
                    coefficient,
                    BigDecimal::add);
        }

        terms.entrySet().removeIf(
                e -> e.getValue().signum() == 0);

        return terms;
    }

    /**
     * Scales small objective coefficients without changing the optimum.
     */
    private static double[] normalizeObjective(double[] objective) {
        double maximum = 0.0d;

        for (double coefficient : objective) {
            if (!Double.isFinite(coefficient)) {
                throw new IllegalStateException(
                        "Non-finite objective coefficient.");
            }

            maximum = Math.max(maximum, Math.abs(coefficient));
        }

        // Preserve zero objectives and sufficiently scaled objectives.
        if (maximum == 0.0d || maximum >= 1.0d) {
            return objective;
        }

        int exponent = Math.getExponent(maximum);

        // Determine the actual exponent of subnormal values.
        if (maximum < Double.MIN_NORMAL) {
            exponent = Math.getExponent(
                    Math.scalb(maximum, 54)) - 54;
        }

        final double[] normalized = objective.clone();

        // Scale by a positive power of two.
        for (int j = 0; j < normalized.length; j++) {
            normalized[j] = Math.scalb(
                    normalized[j], -exponent);
        }

        return normalized;
    }

    private static LPSolution failedSolution(
            List<Variable> vars) {

        final Map<String, Double> values =
                new LinkedHashMap<>();

        for (Variable variable : vars) {
            values.put(variable.getName(), 0.0d);
        }

        return new LPSolution(
                values, Double.NaN, false);
    }
}
