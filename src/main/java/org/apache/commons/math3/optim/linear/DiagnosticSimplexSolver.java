package org.apache.commons.math3.optim.linear;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;

import java.util.Locale;

public final class DiagnosticSimplexSolver extends SimplexSolver {



    private boolean phase1;


    private SimplexTableau observedTableau;
    private boolean phase2Started;



    @Override
    public org.apache.commons.math3.optim.PointValuePair doOptimize() {

        final SimplexTableau tableau = new SimplexTableau(
                getFunction(),
                getConstraints(),
                getGoalType(),
                isRestrictedToNonNegative(),
                1e-6,
                10);


        captureInitialConstraints(tableau);

        solvePhase1(tableau);


        cleanupArtificialBasis(tableau);

        if (tableau.getNumObjectiveFunctions() == 2) {
            dumpColumns("PHASE1_CLEANED", tableau);
        }

        final int oldObjectiveRows =
                tableau.getNumObjectiveFunctions();

        final java.util.List<String> previousBasis =
                new java.util.ArrayList<>();

        final java.util.List<?> oldLabels =
                readColumnLabels(tableau);

        // Preserve the actual basis before removing columns.
        for (int i = oldObjectiveRows;
                i < tableau.getHeight(); i++) {

            final int column = tableau.getBasicVariable(i);

            if (column < oldObjectiveRows
                    || column >= tableau.getRhsOffset()
                    || !Integer.valueOf(i).equals(
                            tableau.getBasicRow(column))) {

                throw new IllegalStateException(
                        "Invalid basis before phase transition");
            }

            previousBasis.add(
                    oldLabels.get(column).toString());
        }


        final boolean retainColumns =
                !"0".equals(System.getenv("RETAIN_COLUMNS"));

        System.err.println(
                "Retain non-artificial columns: "
                        + retainColumns);

        if (retainColumns && oldObjectiveRows == 2) {

            int protectedColumns = 0;

            for (int j = tableau.getNumObjectiveFunctions();
                    j < tableau.getArtificialVariableOffset();
                    j++) {

                if (tableau.getEntry(0, j) > 1e-6) {
                    protectedColumns++;
                }

                // Disable cost-based column elimination only.
                tableau.setEntry(0, j, 0.0d);
            }

            System.err.println(
                    "Protected positive-cost columns: "
                            + protectedColumns);
        }

        tableau.dropPhase1Objective();


        final boolean preserve =
                !"0".equals(
                        System.getenv("PRESERVE_PHASE1_BASIS"));

        System.err.println("Preserve phase 1 basis: " + preserve);

        if (preserve && oldObjectiveRows == 2) {
            restoreBasis(tableau, previousBasis);
        }

        observedTableau = tableau;
        phase2Started = true;

        dumpColumns("PHASE2_START", tableau);


        final java.util.List<Integer> artificialColumns =
                new java.util.ArrayList<>();

        final java.util.List<?> phase2Labels =
                readColumnLabels(tableau);

        for (int j = tableau.getNumObjectiveFunctions();
                j < tableau.getRhsOffset(); j++) {

            if (phase2Labels.get(j).toString().startsWith("a")) {
                artificialColumns.add(j);
            }
        }

        System.err.println(
                "Restricted Phase 2 artificial columns: "
                        + artificialColumns.size());

        while (true) {

            enforceArtificialZero(tableau, artificialColumns);

            if (tableau.isOptimal()) {
                break;
            }

            doIteration(tableau);
        }



        if (rhsRepairs > 0) {

            reconstructBasisSolution(tableau);

            enforceArtificialZero(
                    tableau, artificialColumns);
        }

        final org.apache.commons.math3.optim.PointValuePair result =
                tableau.getSolution();

        if (isRestrictedToNonNegative()) {

            for (double value : result.getPoint()) {
                if (value < -1e-6) {
                    throw new NoFeasibleSolutionException();
                }
            }
        }

        dumpColumns("PHASE2_FINAL", tableau);

        System.err.println(
                "Phase 2 tableau objective: "
                        + tableau.getEntry(
                                0, tableau.getRhsOffset()));

        System.err.println(
                "Phase 2 returned objective: "
                        + result.getValue());

        return result;
    }



    private boolean hasFeasiblePhase1Basis(
            SimplexTableau tableau) {

        if (tableau.getNumObjectiveFunctions() != 2) {
            return false;
        }

        final double tolerance = 1e-12;

        final int rhsColumn = tableau.getRhsOffset();

        final double w = tableau.getEntry(0, rhsColumn);

        if (!Double.isFinite(w)
                || Math.abs(w) > tolerance) {
            return false;
        }

        // Verify primal feasibility of all basic variables.
        for (int i = tableau.getNumObjectiveFunctions();
                i < tableau.getHeight(); i++) {

            final double rhs = tableau.getEntry(i, rhsColumn);

            if (!Double.isFinite(rhs)
                    || rhs < -tolerance) {
                return false;
            }
        }

        final int start = tableau.getArtificialVariableOffset();
        final int end = start + tableau.getNumArtificialVariables();

        double artificialSum = 0.0d;

        for (int j = start; j < end; j++) {

            final Integer row = tableau.getBasicRow(j);

            if (row == null) {
                continue;
            }

            artificialSum += Math.abs(
                    tableau.getEntry(row, rhsColumn));
        }

        return Double.isFinite(artificialSum)
                && artificialSum <= tolerance;
    }



    private void solvePhase1WithFeasibleExit(
            SimplexTableau tableau) {

        if (tableau.getNumArtificialVariables() == 0) {
            super.solvePhase1(tableau);
            return;
        }

        while (true) {

            if (hasFeasiblePhase1Basis(tableau)) {

                System.err.println(
                        "Phase 1 completed with verified zero "
                                + "artificial variables.");

                return;
            }

            boolean improving = false;

            for (int j = tableau.getNumObjectiveFunctions();
                    j < tableau.getArtificialVariableOffset(); j++) {

                if (tableau.getBasicRow(j) == null
                        && tableau.getEntry(0, j) < -1e-12) {

                    improving = true;
                    break;
                }
            }

            if (!improving && tableau.isOptimal()) {

                super.solvePhase1(tableau);

                if (!hasFeasiblePhase1Basis(tableau)) {
                    throw new IllegalStateException(
                            "Phase 1 feasibility not established");
                }

                return;
            }

            doIteration(tableau);
        }
    }


    private void cleanupArtificialBasis(SimplexTableau tableau) {

        if (tableau.getNumArtificialVariables() == 0) {
            return;
        }

        final int firstRow = tableau.getNumObjectiveFunctions();
        final int artificialStart =
                tableau.getArtificialVariableOffset();

        final int rhsColumn = tableau.getRhsOffset();
        final int height = tableau.getHeight();

        final double cutoff = 1e-10;
        final double zeroTolerance = 1e-12;
        final double guardTolerance = 1e-11;

        int removed = 0;

        while (true) {

            int pivotRow = -1;
            int pivotColumn = -1;
            double largestPivot = cutoff;

            for (int i = firstRow; i < height; i++) {

                final int basic = tableau.getBasicVariable(i);

                if (basic < artificialStart) {
                    continue;
                }

                final double rhs =
                        tableau.getEntry(i, rhsColumn);

                if (!Double.isFinite(rhs)
                        || Math.abs(rhs) > zeroTolerance) {

                    throw new IllegalStateException(
                            "Nonzero artificial basis variable: "
                                    + rhs);
                }

                for (int j = firstRow; j < artificialStart; j++) {

                    if (tableau.getBasicRow(j) != null) {
                        continue;
                    }

                    final double entry =
                            tableau.getEntry(i, j);

                    final double magnitude = Math.abs(entry);

                    if (!Double.isFinite(magnitude)
                            || magnitude <= largestPivot) {
                        continue;
                    }

                    final double step = rhs / entry;

                    if (!Double.isFinite(step)
                            || step < -zeroTolerance
                            || Math.abs(step) > 1e-10) {
                        continue;
                    }

                    boolean admissible = true;

                    // Preserve primal feasibility during cleanup.
                    for (int k = firstRow; k < height; k++) {

                        if (k == i) {
                            continue;
                        }

                        final double predicted = FusedArithmetic.fma(
                                -tableau.getEntry(k, j),
                                step,
                                tableau.getEntry(k, rhsColumn));

                        if (!Double.isFinite(predicted)
                                || predicted < -guardTolerance) {

                            admissible = false;
                            break;
                        }
                    }

                    if (admissible) {
                        pivotRow = i;
                        pivotColumn = j;
                        largestPivot = magnitude;
                    }
                }

                // Use row-wise cleanup for lower-shift LPs.
                if (lowerShiftStrategy && pivotRow >= 0) {
                    break;
                }
            }

            if (pivotRow < 0) {
                break;
            }

            incrementIterationCount();

            tableau.performRowOperations(
                    pivotColumn, pivotRow);

            removed++;
        }

        int remaining = 0;
        double maxRemainingCoefficient = 0.0d;

        for (int i = firstRow; i < height; i++) {

            if (tableau.getBasicVariable(i)
                    < artificialStart) {
                continue;
            }

            remaining++;

            final double rhs =
                    tableau.getEntry(i, rhsColumn);

            if (!Double.isFinite(rhs)
                    || Math.abs(rhs) > zeroTolerance) {

                throw new IllegalStateException(
                        "Artificial cleanup lost feasibility");
            }

            for (int j = firstRow; j < artificialStart; j++) {

                final double coefficient =
                        Math.abs(tableau.getEntry(i, j));

                if (!Double.isFinite(coefficient)) {
                    throw new IllegalStateException(
                            "Non-finite cleanup coefficient");
                }

                maxRemainingCoefficient = Math.max(
                        maxRemainingCoefficient,
                        coefficient);
            }
        }

        System.err.println(
                "Artificial cleanup pivots: " + removed);

        System.err.println(
                "Remaining artificial basis variables: "
                        + remaining);

        System.err.println(
                "Maximum remaining non-artificial coefficient: "
                        + maxRemainingCoefficient);

        // Do not silently classify nonzero rows as redundant.
        if (maxRemainingCoefficient > zeroTolerance) {

            throw new IllegalStateException(
                    "Unable to eliminate artificial basis safely");
        }
    }


    private void enforceArtificialZero(
            SimplexTableau tableau,
            java.util.List<Integer> artificialColumns) {

        double sum = 0.0d;

        for (int column : artificialColumns) {

            final Integer row = tableau.getBasicRow(column);

            if (row == null) {

                // Fixed nonbasic artificial variables cannot enter.
                tableau.setEntry(0, column, 0.0d);
                continue;
            }

            final double value = tableau.getEntry(
                    row, tableau.getRhsOffset());

            if (!Double.isFinite(value)) {

                throw new IllegalStateException(
                        "Non-finite artificial variable");
            }

            sum += Math.abs(value);
        }

        if (sum > 1e-12) {

            throw new IllegalStateException(
                    "Artificial variables became nonzero in Phase 2: "
                            + sum);
        }
    }

    private Object readTableauField(
            SimplexTableau tableau,
            String name) {

        try {

            final java.lang.reflect.Field field =
                    SimplexTableau.class.getDeclaredField(name);

            field.setAccessible(true);

            return field.get(tableau);

        } catch (ReflectiveOperationException ex) {

            throw new IllegalStateException(
                    "Cannot access tableau field: " + name,
                    ex);
        }
    }

    private java.util.List<?> readColumnLabels(
            SimplexTableau tableau) {

        return (java.util.List<?>)
                readTableauField(tableau, "columnLabels");
    }

    private void restoreBasis(
            SimplexTableau tableau,
            java.util.List<String> previousBasis) {

        final java.util.List<?> labels =
                readColumnLabels(tableau);

        final int[] columnToRow = (int[])
                readTableauField(tableau, "basicVariables");

        final int[] rowToColumn = (int[])
                readTableauField(tableau, "basicRows");

        final int firstRow =
                tableau.getNumObjectiveFunctions();

        if (previousBasis.size()
                != tableau.getHeight() - firstRow) {

            throw new IllegalStateException(
                    "Unexpected number of basis rows");
        }

        final int[] restoredColumns =
                new int[previousBasis.size()];

        final java.util.Set<Integer> usedColumns =
                new java.util.HashSet<>();

        double maximumUnitResidual = 0.0d;

        // Validate the preserved basis before modifying mappings.
        for (int k = 0; k < previousBasis.size(); k++) {

            final String name = previousBasis.get(k);
            final int column = labels.indexOf(name);
            final int row = firstRow + k;

            if (column < firstRow
                    || column >= tableau.getRhsOffset()) {

                throw new IllegalStateException(
                        "Previous basic column disappeared: "
                                + name);
            }

            if (!usedColumns.add(column)) {
                throw new IllegalStateException(
                        "Duplicate preserved column: " + name);
            }

            restoredColumns[k] = column;

            for (int i = 0; i < tableau.getHeight(); i++) {

                final double expected =
                        i == row ? 1.0d : 0.0d;

                final double error = Math.abs(
                        tableau.getEntry(i, column) - expected);

                if (!Double.isFinite(error)) {
                    throw new IllegalStateException(
                            "Non-finite basis residual");
                }

                maximumUnitResidual = Math.max(
                        maximumUnitResidual, error);
            }
        }

        System.err.println(
                "Preserved basis maximum unit residual: "
                        + maximumUnitResidual);

        if (maximumUnitResidual > 1e-8) {

            throw new IllegalStateException(
                    "Preserved basis is not canonical");
        }

        // Rebuild both mappings using the original basic columns.
        java.util.Arrays.fill(columnToRow, -1);
        java.util.Arrays.fill(rowToColumn, -1);

        for (int k = 0; k < restoredColumns.length; k++) {

            final int row = firstRow + k;
            final int column = restoredColumns[k];

            columnToRow[column] = row;
            rowToColumn[row] = column;
        }

        System.err.println(
                "Restored basis rows: "
                        + restoredColumns.length);
    }

    private void checkOptimalPoint(
            String stage,
            SimplexTableau tableau,
            java.util.List<?> labels)
            throws java.io.IOException {

        final String path = System.getenv("TABLEAU_PROBE");

        if (path == null) {
            return;
        }

        final java.util.Map<String, Double> values =
                new java.util.HashMap<>();

        for (String line : java.nio.file.Files.readAllLines(
                java.nio.file.Paths.get(path))) {

            final String[] parts = line.split("\t");

            values.put(
                    parts[0],
                    Double.parseDouble(parts[1]));
        }

        final int rhsColumn = tableau.getRhsOffset();
        final double[] candidate = new double[rhsColumn];

        for (int j = 0; j < rhsColumn; j++) {

            final String name = labels.get(j).toString();

            if (name.startsWith("a")) {

                // All artificial variables must be zero.
                candidate[j] = 0.0d;

            } else {

                final Double value = values.get(name);

                if (value == null) {
                    throw new IllegalStateException(
                            "Missing candidate value: " + name);
                }

                candidate[j] = value;
            }
        }

        double maxResidual = 0.0d;
        double maxRelativeResidual = 0.0d;
        int worstRow = -1;
        int violatedRows = 0;

        for (int i = 0; i < tableau.getHeight(); i++) {

            double sum = 0.0d;
            double compensation = 0.0d;
            double magnitude = 0.0d;

            // Evaluate the equation using compensated summation.
            for (int j = 0; j < rhsColumn; j++) {

                final double term =
                        tableau.getEntry(i, j) * candidate[j];

                final double corrected = term - compensation;
                final double next = sum + corrected;

                compensation = (next - sum) - corrected;
                sum = next;

                magnitude += Math.abs(term);
            }

            final double rhs = tableau.getEntry(i, rhsColumn);
            final double residual = Math.abs(sum - rhs);

            final double relativeResidual =
                    residual / Math.max(
                            1.0d,
                            magnitude + Math.abs(rhs));

            if (!Double.isFinite(residual)
                    || !Double.isFinite(relativeResidual)) {
                throw new IllegalStateException(
                        "Non-finite tableau residual");
            }

            if (residual > maxResidual) {
                maxResidual = residual;
                worstRow = i;
            }

            maxRelativeResidual = Math.max(
                    maxRelativeResidual,
                    relativeResidual);

            if (residual > 1e-6) {
                violatedRows++;
            }

            if (i < tableau.getNumObjectiveFunctions()) {

                System.err.printf(
                        java.util.Locale.ROOT,
                        "PROBE %s | objectiveRow=%d"
                                + " | lhs=%.12e"
                                + " | rhs=%.12e"
                                + " | residual=%.12e%n",
                        stage,
                        i,
                        sum,
                        rhs,
                        residual);
            }
        }

        // Verify the basic-variable mappings.
        final int[] rowOwners = new int[tableau.getHeight()];

        int basicColumns = 0;
        int mappingMismatches = 0;

        for (int j = tableau.getNumObjectiveFunctions();
                j < rhsColumn; j++) {

            final Integer row = tableau.getBasicRow(j);

            if (row == null) {
                continue;
            }

            basicColumns++;
            rowOwners[row]++;

            if (tableau.getBasicVariable(row) != j) {
                mappingMismatches++;
            }
        }

        int missingRows = 0;
        int duplicateRows = 0;

        for (int i = tableau.getNumObjectiveFunctions();
                i < tableau.getHeight(); i++) {

            if (rowOwners[i] == 0) {
                missingRows++;
            }

            if (rowOwners[i] > 1) {
                duplicateRows++;
            }
        }

        System.err.printf(
                java.util.Locale.ROOT,
                "PROBE %s | maxResidual=%.12e"
                        + " | maxRelativeResidual=%.12e"
                        + " | worstRow=%d"
                        + " | violatedRows=%d%n",
                stage,
                maxResidual,
                maxRelativeResidual,
                worstRow,
                violatedRows);

        System.err.printf(
                java.util.Locale.ROOT,
                "PROBE %s | basicColumns=%d"
                        + " | missingBasisRows=%d"
                        + " | duplicateBasisRows=%d"
                        + " | mappingMismatches=%d%n",
                stage,
                basicColumns,
                missingRows,
                duplicateRows,
                mappingMismatches);
    }

    private void dumpColumns(
            String stage,
            SimplexTableau tableau) {

        if (System.getenv("TABLEAU_PROBE") == null) {
            return;
        }


        try {

            // Read column labels without modifying the tableau.
            final java.lang.reflect.Field field =
                    SimplexTableau.class.getDeclaredField(
                            "columnLabels");

            field.setAccessible(true);

            final java.util.List<?> labels =
                    (java.util.List<?>) field.get(tableau);

            checkOptimalPoint(stage, tableau, labels);


            final int rhsColumn = tableau.getRhsOffset();

            for (int j = tableau.getNumObjectiveFunctions();
                    j < rhsColumn; j++) {

                final Integer basicRow =
                        tableau.getBasicRow(j);

                final double basicValue =
                        basicRow == null
                                ? 0.0d
                                : tableau.getEntry(
                                        basicRow, rhsColumn);

                System.err.printf(
                        java.util.Locale.ROOT,
                        "TABLEAU_COLUMN\\t%s\\t%s\\t%.17g\\t%s\\t%.17g%n",
                        stage,
                        labels.get(j),
                        tableau.getEntry(0, j),
                        basicRow != null,
                        basicValue);
            }

        } catch (ReflectiveOperationException | java.io.IOException ex) {

            throw new IllegalStateException(
                    "Unable to inspect tableau columns.", ex);
        }
    }


    private final boolean blockArtificial =
            !"0".equals(System.getenv("BLOCK_ARTIFICIAL"));

    private int artificialEntries = 0;
    private int artificialExits = 0;
    private long blockedCandidates = 0;

    private double bestW = Double.NEGATIVE_INFINITY;
    private int lastProgressIteration = 0;
    private int repeatedBases = 0;

    private final java.util.Set<java.util.BitSet> visitedBases =
            new java.util.HashSet<>();


    public DiagnosticSimplexSolver() {
        super(
                1e-6,
                10,
                Double.parseDouble(
                        System.getProperty(
                                "benchmark.cutoff", "1e-10"))
        );

        
        System.out.println("Using Harris v2");
        System.out.println("Block artificial reentry: "
                + blockArtificial);

        System.out.println("Pivot cutoff: "
                + System.getProperty(
                        "benchmark.cutoff", "1e-10"));
    }

    @Override
    protected void solvePhase1(SimplexTableau tableau) {
        phase1 = true;

        report(tableau, "PHASE 1 START");
        dumpColumns("PHASE1_START", tableau);

        try {
            if (lowerShiftStrategy) {
                super.solvePhase1(tableau);
            } else {
                solvePhase1WithFeasibleExit(tableau);
            }
        } finally {


            report(tableau, "PHASE 1 END");

            observedTableau = tableau;

            if (tableau.getNumObjectiveFunctions() == 2) {
                dumpColumns("PHASE1_FINAL", tableau);
            }


            System.err.println(
                    "FINAL PIVOT STATISTICS"
                            + " | artificialEntries="
                            + artificialEntries
                            + " | artificialExits="
                            + artificialExits
                            + " | blockedCandidates="
                            + blockedCandidates
                            + " | repeatedBases="
                            + repeatedBases);

            phase1 = false;
        }
    }

    @Override
    protected void doIteration(SimplexTableau tableau) {

        if (tableau.getNumObjectiveFunctions() == 1
                && observedTableau != null
                && !phase2Started) {

            phase2Started = true;
            dumpColumns("PHASE2_START", tableau);
        }

        stableIteration(tableau);


        if (phase1 && getIterations() % 1000 == 0) {
            report(tableau, "PHASE 1 PROGRESS");
        }
    }





    private org.apache.commons.math3.linear.OpenMapRealMatrix
            initialConstraints;

    private double[] initialRhs;
    private double[] initialObjective;
    private double initialObjectiveRhs;
    private int dualRepairPivots;


    private java.util.Map<String, Integer> initialColumns;


    private void captureInitialConstraints(
            SimplexTableau tableau) {

        final int firstRow =
                tableau.getNumObjectiveFunctions();

        final int rhsColumn = tableau.getRhsOffset();
        final int rows = tableau.getHeight() - firstRow;

        initialConstraints =
                new org.apache.commons.math3.linear.OpenMapRealMatrix(
                        rows, rhsColumn);

        initialRhs = new double[rows];
        initialObjective = new double[rhsColumn];

        final int objectiveRow = firstRow - 1;

        initialObjectiveRhs =
                tableau.getEntry(objectiveRow, rhsColumn);

        for (int j = 0; j < rhsColumn; j++) {
            initialObjective[j] =
                    tableau.getEntry(objectiveRow, j);
        }


        initialColumns = new java.util.HashMap<>();

        final java.util.List<?> labels =
                readColumnLabels(tableau);

        for (int j = 0; j < rhsColumn; j++) {
            initialColumns.put(
                    labels.get(j).toString(), j);
        }

        for (int i = 0; i < rows; i++) {

            initialRhs[i] =
                    tableau.getEntry(i + firstRow, rhsColumn);

            for (int j = 0; j < rhsColumn; j++) {

                final double value =
                        tableau.getEntry(i + firstRow, j);

                // Store only nonzero coefficients.
                if (value != 0.0d) {
                    initialConstraints.setEntry(i, j, value);
                }
            }
        }

        System.err.println(
                "BASIS_RECONSTRUCTION captured rows=" + rows);
    }


    private double[] calculateBasisResidual(
            double[][] basis,
            double[] values) {

        final double[] residual =
                new double[initialRhs.length];

        // Evaluate residuals using exact double representations.
        for (int i = 0; i < basis.length; i++) {

            java.math.BigDecimal sum =
                    java.math.BigDecimal.ZERO;

            for (int j = 0; j < values.length; j++) {

                if (basis[i][j] == 0.0d
                        || values[j] == 0.0d) {
                    continue;
                }

                sum = sum.add(
                        new java.math.BigDecimal(basis[i][j])
                                .multiply(
                                    new java.math.BigDecimal(
                                            values[j])));
            }

            residual[i] =
                    new java.math.BigDecimal(initialRhs[i])
                            .subtract(sum)
                            .doubleValue();
        }

        return residual;
    }


    private double maximumAbsolute(double[] values) {

        double maximum = 0.0d;

        for (double value : values) {

            if (!Double.isFinite(value)) {
                throw new IllegalStateException(
                        "Non-finite basis value");
            }

            maximum = Math.max(maximum, Math.abs(value));
        }

        return maximum;
    }



    private void refreshOriginalTableau(
            SimplexTableau tableau,
            DecompositionSolver solver,
            double[] values) {

        final long start = System.nanoTime();

        final int firstRow = tableau.getNumObjectiveFunctions();
        final int rhsColumn = tableau.getRhsOffset();
        final int rows = values.length;

        if (firstRow != 1) {
            throw new IllegalStateException(
                    "Expected Phase 2 tableau");
        }

        final java.util.List<?> labels =
                readColumnLabels(tableau);

        final double[] basicCosts = new double[rows];

        for (int i = 0; i < rows; i++) {

            final int column =
                    tableau.getBasicVariable(i + firstRow);

            final String name =
                    labels.get(column).toString();

            basicCosts[i] =
                    initialObjective[initialColumns.get(name)];
        }

        double maximumDualViolation = 0.0d;

        for (int j = firstRow; j < rhsColumn; j++) {

            final Integer basicRow =
                    tableau.getBasicRow(j);

            final String name =
                    labels.get(j).toString();

            final int originalColumn =
                    initialColumns.get(name);

            // Restore canonical basic columns.
            if (basicRow != null) {

                for (int i = 0; i < rows; i++) {

                    tableau.setEntry(
                            i + firstRow,
                            j,
                            i + firstRow == basicRow
                                    ? 1.0d : 0.0d);
                }

                tableau.setEntry(0, j, 0.0d);
                continue;
            }

            final double[] column = new double[rows];

            for (int i = 0; i < rows; i++) {
                column[i] =
                        initialConstraints.getEntry(i, originalColumn);
            }

            // Recompute the tableau column as B^-1 A_j.
            final double[] transformed =
                    solver.solve(
                            new ArrayRealVector(column, false))
                            .toArray();

            double reducedCost =
                    initialObjective[originalColumn];

            for (int i = 0; i < rows; i++) {

                if (!Double.isFinite(transformed[i])) {
                    throw new IllegalStateException(
                            "Non-finite reconstructed column");
                }

                tableau.setEntry(
                        i + firstRow, j, transformed[i]);

                reducedCost = FusedArithmetic.fma(
                        -basicCosts[i],
                        transformed[i],
                        reducedCost);
            }

            if (!Double.isFinite(reducedCost)) {
                throw new IllegalStateException(
                        "Non-finite reconstructed reduced cost");
            }

            tableau.setEntry(0, j, reducedCost);

            // Artificial variables are fixed at zero.
            if (!name.startsWith("a")) {

                maximumDualViolation = Math.max(
                        maximumDualViolation,
                        -reducedCost);
            }
        }

        double objectiveRhs = initialObjectiveRhs;

        for (int i = 0; i < rows; i++) {

            tableau.setEntry(
                    i + firstRow,
                    rhsColumn,
                    values[i]);

            objectiveRhs = FusedArithmetic.fma(
                    -basicCosts[i],
                    values[i],
                    objectiveRhs);
        }

        tableau.setEntry(0, rhsColumn, objectiveRhs);

        System.err.println(
                "DUAL_REPAIR maximumDualViolation="
                        + maximumDualViolation);

        System.err.println(
                "DUAL_REPAIR refreshSeconds="
                        + (System.nanoTime() - start) / 1e9);

        if (maximumDualViolation > 1e-6) {

            throw new IllegalStateException(
                    "Reconstructed basis is not dual feasible");
        }
    }


    private void repairFinalBasis(
            SimplexTableau tableau,
            DecompositionSolver solver,
            double[] values) {

        if (dualRepairPivots >= 8) {

            throw new IllegalStateException(
                    "Dual repair pivot limit exceeded");
        }

        refreshOriginalTableau(tableau, solver, values);

        final int firstRow = tableau.getNumObjectiveFunctions();
        final int rhsColumn = tableau.getRhsOffset();

        int leavingRow = -1;
        double minimumValue = -1e-14;

        // Select an infeasible basic variable.
        for (int i = 0; i < values.length; i++) {

            if (values[i] < minimumValue) {

                minimumValue = values[i];
                leavingRow = i + firstRow;
            }
        }

        if (leavingRow < 0) {
            return;
        }

        final java.util.List<?> labels =
                readColumnLabels(tableau);

        int enteringColumn = -1;
        double bestRatio = Double.POSITIVE_INFINITY;
        double bestPivot = 0.0d;

        // Apply the dual simplex ratio test.
        for (int j = firstRow; j < rhsColumn; j++) {

            if (tableau.getBasicRow(j) != null
                    || labels.get(j).toString().startsWith("a")) {
                continue;
            }

            final double entry =
                    tableau.getEntry(leavingRow, j);

            if (entry >= -1e-10) {
                continue;
            }

            final double cost =
                    tableau.getEntry(0, j);

            final double ratio =
                    Math.max(0.0d, cost) / (-entry);

            if (!Double.isFinite(ratio)) {
                continue;
            }

            if (ratio < bestRatio
                    || (ratio == bestRatio
                        && -entry > bestPivot)) {

                enteringColumn = j;
                bestRatio = ratio;
                bestPivot = -entry;
            }
        }

        if (enteringColumn < 0) {

            throw new IllegalStateException(
                    "No admissible dual repair pivot");
        }

        System.err.println(
                "DUAL_REPAIR pivot="
                        + (dualRepairPivots + 1)
                        + " | leaving="
                        + labels.get(
                            tableau.getBasicVariable(leavingRow))
                        + " | entering="
                        + labels.get(enteringColumn)
                        + " | negativeValue="
                        + minimumValue
                        + " | ratio="
                        + bestRatio
                        + " | pivotMagnitude="
                        + bestPivot);

        incrementIterationCount();

        tableau.performRowOperations(
                enteringColumn, leavingRow);

        dualRepairPivots++;
    }

    private void reconstructBasisSolution(
            SimplexTableau tableau) {

        final long start = System.nanoTime();

        final int firstRow =
                tableau.getNumObjectiveFunctions();

        final int rhsColumn = tableau.getRhsOffset();
        final int rows = tableau.getHeight() - firstRow;

        if (rows != initialRhs.length) {

            throw new IllegalStateException(
                    "Constraint count changed during optimization");
        }

        final java.util.List<?> labels =
                readColumnLabels(tableau);

        final double[][] basis = new double[rows][rows];
        final double[] previousValues = new double[rows];

        final java.util.Set<Integer> usedColumns =
                new java.util.HashSet<>();

        // Reconstruct the basis from the original constraints.
        for (int j = 0; j < rows; j++) {

            final int column =
                    tableau.getBasicVariable(j + firstRow);

            if (column < firstRow
                    || column >= rhsColumn
                    || !usedColumns.add(column)
                    || !Integer.valueOf(j + firstRow).equals(
                            tableau.getBasicRow(column))) {

                throw new IllegalStateException(
                        "Invalid final basis mapping");
            }

            final String name = labels.get(column).toString();

            final Integer originalColumn =
                    initialColumns.get(name);

            if (originalColumn == null) {

                throw new IllegalStateException(
                        "Missing original basis column: " + name);
            }

            previousValues[j] =
                    tableau.getEntry(j + firstRow, rhsColumn);

            for (int i = 0; i < rows; i++) {

                basis[i][j] =
                        initialConstraints.getEntry(i, originalColumn);
            }
        }

        final double previousResidual = maximumAbsolute(
                calculateBasisResidual(basis, previousValues));

        final org.apache.commons.math3.linear.RealMatrix matrix =
                new org.apache.commons.math3.linear
                        .Array2DRowRealMatrix(basis, false);

        final org.apache.commons.math3.linear.DecompositionSolver solver =
                new org.apache.commons.math3.linear
                        .LUDecomposition(matrix, 1e-14)
                        .getSolver();

        if (!solver.isNonSingular()) {

            throw new IllegalStateException(
                    "Numerically singular final basis");
        }

        double[] values = solver.solve(
                new org.apache.commons.math3.linear
                        .ArrayRealVector(initialRhs, true))
                .toArray();

        double[] residual =
                calculateBasisResidual(basis, values);

        double error = maximumAbsolute(residual);

        // Improve the basis solution using iterative refinement.
        for (int iteration = 0; iteration < 3; iteration++) {

            if (error == 0.0d) {
                break;
            }

            final double[] correction = solver.solve(
                    new org.apache.commons.math3.linear
                            .ArrayRealVector(residual, false))
                    .toArray();

            final double[] candidate = values.clone();

            for (int i = 0; i < rows; i++) {
                candidate[i] += correction[i];
            }

            final double[] nextResidual =
                    calculateBasisResidual(basis, candidate);

            final double nextError =
                    maximumAbsolute(nextResidual);

            if (nextError >= error) {
                break;
            }

            values = candidate;
            residual = nextResidual;
            error = nextError;
        }

        double minimumValue = 0.0d;
        double maximumChange = 0.0d;

        for (int i = 0; i < rows; i++) {

            minimumValue = Math.min(minimumValue, values[i]);

            // Reject materially infeasible basic values.
            if (values[i] < -1e-14) {

                System.err.println(
                        "BASIS_RECONSTRUCTION negative value="
                                + values[i]);


                repairFinalBasis(tableau, solver, values);

                reconstructBasisSolution(tableau);

                return;
            }

            // Remove negligible negative round-off errors.
            if (values[i] < 0.0d) {
                values[i] = 0.0d;
            }

            maximumChange = Math.max(
                    maximumChange,
                    Math.abs(values[i] - previousValues[i]));
        }


        if (dualRepairPivots > 0) {

            refreshOriginalTableau(
                    tableau, solver, values);

            System.err.println(
                    "DUAL_REPAIR completedPivots="
                            + dualRepairPivots);
        }

        final double finalResidual = maximumAbsolute(
                calculateBasisResidual(basis, values));

        System.err.println(
                "BASIS_RECONSTRUCTION previousResidual="
                        + previousResidual);

        System.err.println(
                "BASIS_RECONSTRUCTION finalResidual="
                        + finalResidual);

        System.err.println(
                "BASIS_RECONSTRUCTION minimumValue="
                        + minimumValue);

        System.err.println(
                "BASIS_RECONSTRUCTION maximumChange="
                        + maximumChange);

        if (finalResidual > 1e-12) {

            throw new IllegalStateException(
                    "Reconstructed basis residual too large");
        }

        final double oldObjective =
                tableau.getSolution().getValue();

        // Replace the basic values only after validation.
        for (int i = 0; i < rows; i++) {

            tableau.setEntry(
                    i + firstRow, rhsColumn, values[i]);
        }

        final double newObjective =
                tableau.getSolution().getValue();

        final double objectiveSign = tableau.getEntry(0, 0);

        if (!Double.isFinite(newObjective)
                || Math.abs(objectiveSign) != 1.0d) {

            throw new IllegalStateException(
                    "Invalid reconstructed objective");
        }

        tableau.setEntry(
                0, rhsColumn, objectiveSign * newObjective);

        System.err.println(
                "BASIS_RECONSTRUCTION objectiveBefore="
                        + oldObjective);

        System.err.println(
                "BASIS_RECONSTRUCTION objectiveAfter="
                        + newObjective);

        System.err.println(
                "BASIS_RECONSTRUCTION seconds="
                        + (System.nanoTime() - start) / 1e9);
    }


    private boolean lowerShiftStrategy;

    public DiagnosticSimplexSolver withLowerShiftStrategy(
            boolean enabled) {

        this.lowerShiftStrategy = enabled;
        return this;
    }

    private int rhsRepairs;
    private double cumulativeRhsCorrection;

    private static final class NoStablePivotException
            extends IllegalStateException {

        private NoStablePivotException(String message) {
            super(message);
        }
    }


    private void stableIteration(SimplexTableau tableau) {

        try {

            stableIterationAttempt(tableau);

        } catch (NoStablePivotException ex) {

            if (phase1
                    || "0".equals(System.getenv("RHS_REPAIR"))
                    || !repairNegativeRhs(tableau)) {

                throw ex;
            }

            // Retry once with the repaired RHS.
            stableIterationAttempt(tableau);
        }
    }


    private boolean repairNegativeRhs(SimplexTableau tableau) {

        final double tolerance = 1e-11;
        final double correctionBudget = 1e-9;

        if (rhsRepairs >= 16) {
            return false;
        }

        final int firstRow = tableau.getNumObjectiveFunctions();

        if (firstRow != 1) {
            return false;
        }

        final int rhsColumn = tableau.getRhsOffset();

        int correctedRows = 0;
        double totalCorrection = 0.0d;
        double maximumCorrection = 0.0d;

        // Validate every correction before modifying the tableau.
        for (int i = firstRow; i < tableau.getHeight(); i++) {

            final double rhs = tableau.getEntry(i, rhsColumn);

            if (!Double.isFinite(rhs) || rhs < -tolerance) {
                return false;
            }

            if (rhs < 0.0d) {

                correctedRows++;
                totalCorrection += -rhs;

                maximumCorrection = Math.max(
                        maximumCorrection, -rhs);
            }
        }

        if (correctedRows == 0
                || cumulativeRhsCorrection + totalCorrection
                        > correctionBudget) {

            return false;
        }

        final double objectiveSign = tableau.getEntry(0, 0);

        if (Math.abs(objectiveSign) != 1.0d) {
            return false;
        }

        final double oldObjective =
                tableau.getSolution().getValue();

        // Correct small negative basic values only.
        for (int i = firstRow; i < tableau.getHeight(); i++) {

            if (tableau.getEntry(i, rhsColumn) < 0.0d) {
                tableau.setEntry(i, rhsColumn, 0.0d);
            }
        }

        // Update the objective RHS consistently.
        final double newObjective =
                tableau.getSolution().getValue();

        if (!Double.isFinite(newObjective)) {

            throw new IllegalStateException(
                    "Non-finite objective after RHS repair");
        }

        tableau.setEntry(
                0, rhsColumn, objectiveSign * newObjective);

        rhsRepairs++;
        cumulativeRhsCorrection += totalCorrection;

        System.err.println(
                "RHS_REPAIR"
                        + " | number=" + rhsRepairs
                        + " | iteration=" + getIterations()
                        + " | correctedRows=" + correctedRows
                        + " | maximumCorrection=" + maximumCorrection
                        + " | cumulativeCorrection="
                        + cumulativeRhsCorrection
                        + " | objectiveChange="
                        + (newObjective - oldObjective));

        return true;
    }


    private void stableIterationAttempt(SimplexTableau tableau) {


        incrementIterationCount();

        final int firstRow =
                tableau.getNumObjectiveFunctions();

        final int rhsColumn =
                tableau.getRhsOffset();

        final int height = tableau.getHeight();

        final double cutoff = Double.parseDouble(
                System.getProperty(
                        "benchmark.cutoff", "1e-10"));

        final double feasibilityTolerance = 1e-12;
        final double guardTolerance = 1e-11;
        final double optimalityTolerance = lowerShiftStrategy
                ? 1e-6
                : (tableau.getNumObjectiveFunctions() == 2
                        ? 1e-12
                        : 1e-6);


        final int artificialStart =
                tableau.getArtificialVariableOffset();

        final int artificialEnd =
                artificialStart
                        + tableau.getNumArtificialVariables();

        if (phase1) {

            final double w =
                    tableau.getEntry(0, rhsColumn);

            if (w > bestW + 1e-9) {
                bestW = w;
                lastProgressIteration = getIterations();
                visitedBases.clear();
            }

            final int stalledIterations =
                    getIterations() - lastProgressIteration;

            if (stalledIterations >= 100) {

                final java.util.BitSet basis =
                        new java.util.BitSet();

                for (int i = firstRow; i < height; i++) {
                    final int basic =
                            tableau.getBasicVariable(i);

                    if (basic >= 0) {
                        basis.set(basic);
                    }
                }

                if (!visitedBases.add(basis)) {
                    repeatedBases++;
                }
            }

            if (getIterations() % 1000 == 0) {

                System.err.println(
                        "Pivot statistics"
                                + " | artificialEntries="
                                + artificialEntries
                                + " | artificialExits="
                                + artificialExits
                                + " | blockedCandidates="
                                + blockedCandidates
                                + " | repeatedBases="
                                + repeatedBases
                                + " | stalledIterations="
                                + stalledIterations);
            }

            // Stop unproductive experiments early.
            if (stalledIterations >= 3000) {
                throw new IllegalStateException(
                        "Phase 1 stalled for 3000 iterations.");
            }
        }

        // Collect improving non-basic columns.
        final java.util.List<Integer> candidates =
                new java.util.ArrayList<>();

        for (int j = firstRow; j < rhsColumn; j++) {

            if (tableau.getBasicRow(j) != null) {
                continue;
            }

            final double cost = tableau.getEntry(0, j);

            if (!Double.isFinite(cost)) {
                throw new IllegalStateException(
                        "Non-finite reduced cost.");
            }

            if (cost < -optimalityTolerance) {

                final boolean artificial =
                        phase1
                                && j >= artificialStart
                                && j < artificialEnd;

                if (blockArtificial && artificial) {
                    blockedCandidates++;
                    continue;
                }

                candidates.add(j);
            }
        }

        // Try the most promising columns first.
        candidates.sort(
                java.util.Comparator.comparingDouble(
                        j -> tableau.getEntry(0, j)));


        int rejectedColumns = 0;
        int rejectedNoLimit = 0;
        int rejectedNegativeStep = 0;
        int rejectedRatio = 0;
        int rejectedGuard = 0;
        int rejectedNonFinite = 0;

        double minimumRejectedStep = 0.0d;
        double minimumPredictedRhs = 0.0d;
        double largestRejectedPivot = 0.0d;

        int worstGuardRow = -1;
        int worstGuardColumn = -1;


        for (int pivotColumn : candidates) {

            double maximumStep =
                    Double.POSITIVE_INFINITY;

            // First pass: use the same cutoff in both passes.
            for (int i = firstRow; i < height; i++) {

                final double entry =
                        tableau.getEntry(i, pivotColumn);

                final double rhs =
                        tableau.getEntry(i, rhsColumn);

                if (!Double.isFinite(entry)
                        || !Double.isFinite(rhs)) {
                    throw new IllegalStateException(
                            "Non-finite tableau entry.");
                }

                if (entry > cutoff) {

                    final double step =
                            (Math.max(0.0d, rhs)
                                    + feasibilityTolerance)
                                    / entry;

                    maximumStep = Math.min(
                            maximumStep, step);
                }
            }

            if (!Double.isFinite(maximumStep)) {
                rejectedNoLimit++;
                rejectedColumns++;
                continue;
            }

            // Second pass: prefer a stable admissible pivot.
            int pivotRow = -1;
            double largestPivot = 0.0d;

            for (int i = firstRow; i < height; i++) {

                final double entry =
                        tableau.getEntry(i, pivotColumn);

                if (entry <= cutoff
                        || entry <= largestPivot) {
                    continue;
                }

                final double rhs =
                        tableau.getEntry(i, rhsColumn);

                final double step = rhs / entry;


                if (!Double.isFinite(step)) {
                    rejectedNonFinite++;
                    continue;
                }

                if (step < -feasibilityTolerance) {
                    rejectedNegativeStep++;
                    minimumRejectedStep = Math.min(
                            minimumRejectedStep, step);
                    continue;
                }

                if (step > maximumStep) {
                    rejectedRatio++;
                    continue;
                }


                // Check the predicted RHS of every row,
                // including rows below the pivot cutoff.
                boolean admissible = true;

                for (int k = firstRow; k < height; k++) {

                    if (k == i) {
                        continue;
                    }

                    final double predictedRhs = FusedArithmetic.fma(
                            -tableau.getEntry(k, pivotColumn),
                            step,
                            tableau.getEntry(k, rhsColumn));

                    if (!Double.isFinite(predictedRhs)
                            || predictedRhs < -guardTolerance) {

                        rejectedGuard++;

                        largestRejectedPivot = Math.max(
                                largestRejectedPivot, entry);

                        if (predictedRhs < minimumPredictedRhs) {
                            minimumPredictedRhs = predictedRhs;
                            worstGuardRow = k;
                            worstGuardColumn = pivotColumn;
                        }

                        admissible = false;
                        break;
                    }
                }

                if (admissible) {
                    largestPivot = entry;
                    pivotRow = i;
                }
            }

            if (pivotRow < 0) {
                rejectedColumns++;
                continue;
            }

            if (rejectedColumns > 0
                    && (getIterations() < 500
                    || getIterations() % 1000 == 0)) {

                System.err.println(
                        "Alternative pivot selected"
                                + " | iteration="
                                + getIterations()
                                + " | rejectedColumns="
                                + rejectedColumns
                                + " | pivot="
                                + largestPivot);
            }


            if (phase1) {

                final int leaving =
                        tableau.getBasicVariable(pivotRow);

                if (pivotColumn >= artificialStart
                        && pivotColumn < artificialEnd) {
                    artificialEntries++;
                }

                if (leaving >= artificialStart
                        && leaving < artificialEnd) {
                    artificialExits++;
                }
            }

            // Retain Commons Math tableau operations.
            tableau.performRowOperations(
                    pivotColumn, pivotRow);

            return;
        }


        double minimumCurrentRhs = 0.0d;
        double maximumCoefficient = 0.0d;
        int negativeRows = 0;

        for (int i = firstRow; i < height; i++) {

            final double rhs = tableau.getEntry(i, rhsColumn);

            minimumCurrentRhs = Math.min(
                    minimumCurrentRhs, rhs);

            if (rhs < -guardTolerance) {
                negativeRows++;
            }

            for (int j = firstRow; j < rhsColumn; j++) {
                maximumCoefficient = Math.max(
                        maximumCoefficient,
                        Math.abs(tableau.getEntry(i, j)));
            }
        }

        System.err.println("PIVOT_REJECTION phase1=" + phase1);
        System.err.println("PIVOT_REJECTION candidates="
                + candidates.size());
        System.err.println("PIVOT_REJECTION noLimit="
                + rejectedNoLimit);
        System.err.println("PIVOT_REJECTION negativeStep="
                + rejectedNegativeStep);
        System.err.println("PIVOT_REJECTION ratio="
                + rejectedRatio);
        System.err.println("PIVOT_REJECTION guard="
                + rejectedGuard);
        System.err.println("PIVOT_REJECTION nonFinite="
                + rejectedNonFinite);

        System.err.println("PIVOT_REJECTION minimumCurrentRhs="
                + minimumCurrentRhs);
        System.err.println("PIVOT_REJECTION negativeRows="
                + negativeRows);
        System.err.println("PIVOT_REJECTION minimumRejectedStep="
                + minimumRejectedStep);
        System.err.println("PIVOT_REJECTION minimumPredictedRhs="
                + minimumPredictedRhs);
        System.err.println("PIVOT_REJECTION largestRejectedPivot="
                + largestRejectedPivot);
        System.err.println("PIVOT_REJECTION maximumCoefficient="
                + maximumCoefficient);
        System.err.println("PIVOT_REJECTION worstGuardRow="
                + worstGuardRow);
        System.err.println("PIVOT_REJECTION worstGuardColumn="
                + worstGuardColumn);

        throw new NoStablePivotException(
                "No stable pivot found after checking "
                        + candidates.size()
                        + " improving columns.");
    }

    private void report(
            SimplexTableau tableau, String stage) {

        if (tableau.getNumArtificialVariables() == 0) {
            return;
        }

        final int rhsColumn = tableau.getRhsOffset();

        final double w =
                tableau.getEntry(0, rhsColumn);


        double artificialSum = 0.0d;

        final int artificialStart =
                tableau.getArtificialVariableOffset();

        final int artificialEnd =
                artificialStart
                        + tableau.getNumArtificialVariables();

        for (int j = artificialStart;
                j < artificialEnd; j++) {

            final Integer row = tableau.getBasicRow(j);

            if (row != null) {
                artificialSum += tableau.getEntry(
                        row, rhsColumn);
            }
        }

        final double reconstructedW = -artificialSum;
        final double objectiveDrift = w - reconstructedW;

        System.err.println(
                "W consistency: tableau=" + w
                        + ", reconstructed=" + reconstructedW
                        + ", drift=" + objectiveDrift);

        double minimumReducedCost = 0.0d;
        double minimumRhs = 0.0d;
        double negativeRhsSum = 0.0d;

        int negativeRows = 0;

        for (int j = tableau.getNumObjectiveFunctions();
                j < rhsColumn; j++) {

            minimumReducedCost = Math.min(
                    minimumReducedCost,
                    tableau.getEntry(0, j));
        }

        for (int i = tableau.getNumObjectiveFunctions();
                i < tableau.getHeight(); i++) {

            final double rhs =
                    tableau.getEntry(i, rhsColumn);

            minimumRhs = Math.min(minimumRhs, rhs);

            if (rhs < -1e-6) {
                negativeRows++;
                negativeRhsSum += -rhs;
            }
        }

        System.err.printf(
                Locale.ROOT,
                "%s | iterations=%d | W=%.12e"
                        + " | minReducedCost=%.12e"
                        + " | minRhs=%.12e"
                        + " | negativeRows=%d"
                        + " | negativeRhsSum=%.12e%n",
                stage,
                getIterations(),
                w,
                minimumReducedCost,
                minimumRhs,
                negativeRows,
                negativeRhsSum
        );
    }
}
