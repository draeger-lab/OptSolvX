
package org.optsolvx.tests.lp.commonsmath;

import org.junit.jupiter.api.Test;
import org.optsolvx.backend.commonsmath.CommonsMathSolver;
import org.optsolvx.model.AbstractLPModel;
import org.optsolvx.model.Constraint;
import org.optsolvx.model.OptimizationDirection;
import org.optsolvx.solver.LPSolution;
import org.optsolvx.solver.LPSolverAdapter;
import org.optsolvx.tests.lp.BaseLPSolverTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CommonsMathSolverTest extends BaseLPSolverTest {

    private static final double TOL = 1e-6;

    @Override
    protected LPSolverAdapter getSolver() {
        return new CommonsMathSolver();
    }

    @Test
    void solve_requires_build() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, 10.0d);

        assertThrows(
                IllegalStateException.class,
                () -> getSolver().solve(model));
    }

    @Test
    void solve_requires_non_null_model() {
        assertThrows(
                IllegalArgumentException.class,
                () -> getSolver().solve(null));
    }

    @Test
    void testMinimizationWithEquality() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, 10.0d);
        model.addVariable("y", 0.0d, 10.0d);

        model.addConstraint(
                "balance",
                Map.of("x", 1.0d, "y", 1.0d),
                Constraint.Relation.EQ,
                5.0d);

        model.setObjective(
                Map.of("x", 2.0d, "y", 3.0d),
                OptimizationDirection.MINIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(5.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(0.0d, sol.getVariableValues().get("y"), TOL);
        assertEquals(10.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testFixedVariableSubstitution() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 2.0d, 2.0d);
        model.addVariable("y", 0.0d, 10.0d);

        model.addConstraint(
                "balance",
                Map.of("x", 1.0d, "y", 1.0d),
                Constraint.Relation.EQ,
                5.0d);

        model.setObjective(
                Map.of("x", 4.0d, "y", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(2.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(3.0d, sol.getVariableValues().get("y"), TOL);
        assertEquals(11.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testAllVariablesFixed() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 2.0d, 2.0d);
        model.addVariable("y", -3.0d, -3.0d);

        model.addConstraint(
                "balance",
                Map.of("x", 1.0d, "y", 1.0d),
                Constraint.Relation.EQ,
                -1.0d);

        model.setObjective(
                Map.of("x", 2.0d, "y", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(2.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(-3.0d, sol.getVariableValues().get("y"), TOL);
        assertEquals(1.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testNegativeBounds() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", -10.0d, -2.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(-2.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(-2.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testBoundsCrossingZero() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", -5.0d, 10.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MINIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(-5.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(-5.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testInfiniteUpperBound() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, Double.POSITIVE_INFINITY);

        model.addConstraint(
                "limit",
                Map.of("x", 1.0d),
                Constraint.Relation.LEQ,
                7.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(7.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(7.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testInfiniteLowerBound() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", Double.NEGATIVE_INFINITY, 0.0d);

        model.addConstraint(
                "limit",
                Map.of("x", 1.0d),
                Constraint.Relation.GEQ,
                -4.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MINIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(-4.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(-4.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testFreeVariable() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable(
                "x",
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        model.addConstraint(
                "limit",
                Map.of("x", 1.0d),
                Constraint.Relation.LEQ,
                -2.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(-2.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(-2.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testEqualVariableMerging() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, 10.0d);
        model.addVariable("y", 0.0d, 4.0d);

        model.addConstraint(
                "coupling",
                Map.of("x", 1.0d, "y", -1.0d),
                Constraint.Relation.EQ,
                0.0d);

        model.setObjective(
                Map.of("x", 1.0d, "y", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(4.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(4.0d, sol.getVariableValues().get("y"), TOL);
        assertEquals(8.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testOppositeVariableMerging() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", -10.0d, 10.0d);
        model.addVariable("y", -4.0d, 4.0d);

        model.addConstraint(
                "coupling",
                Map.of("x", 1.0d, "y", 1.0d),
                Constraint.Relation.EQ,
                0.0d);

        model.setObjective(
                Map.of("x", 1.0d, "y", -1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(4.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(-4.0d, sol.getVariableValues().get("y"), TOL);
        assertEquals(8.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testZeroFluxPropagation() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, 10.0d);
        model.addVariable("y", 0.0d, 10.0d);
        model.addVariable("z", 0.0d, 10.0d);

        model.addConstraint(
                "balance",
                Map.of("x", 1.0d, "y", 1.0d),
                Constraint.Relation.EQ,
                0.0d);

        model.addConstraint(
                "coupling",
                Map.of("y", 1.0d, "z", -1.0d),
                Constraint.Relation.EQ,
                0.0d);

        model.setObjective(
                Map.of("z", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(0.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(0.0d, sol.getVariableValues().get("y"), TOL);
        assertEquals(0.0d, sol.getVariableValues().get("z"), TOL);
        assertEquals(0.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testInfeasibleModel() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, 1.0d);

        model.addConstraint(
                "impossible",
                Map.of("x", 1.0d),
                Constraint.Relation.GEQ,
                2.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertFalse(sol.isFeasible());
        assertTrue(Double.isNaN(sol.getObjectiveValue()));
    }

    @Test
    void testUnboundedModel() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, Double.POSITIVE_INFINITY);

        model.addConstraint(
                "lower",
                Map.of("x", 1.0d),
                Constraint.Relation.GEQ,
                1.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertFalse(sol.isFeasible());
        assertTrue(Double.isNaN(sol.getObjectiveValue()));
    }

    @Test
    void testDegenerateConstraints() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, 10.0d);
        model.addVariable("y", 0.0d, 10.0d);

        model.addConstraint(
                "balance1",
                Map.of("x", 1.0d, "y", 1.0d),
                Constraint.Relation.EQ,
                4.0d);

        model.addConstraint(
                "balance2",
                Map.of("x", 2.0d, "y", 2.0d),
                Constraint.Relation.EQ,
                8.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(4.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(0.0d, sol.getVariableValues().get("y"), TOL);
        assertEquals(4.0d, sol.getObjectiveValue(), TOL);
    }

    @Test
    void testSmallObjectiveCoefficient() {
        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", 0.0d, 1.0d);

        model.setObjective(
                Map.of("x", 1e-8),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(1.0d, sol.getVariableValues().get("x"), TOL);
        assertEquals(1e-8, sol.getObjectiveValue(), 1e-12);
    }

    @Test
    void testRepeatedSolve() {
        LPSolverAdapter solver = getSolver();

        AbstractLPModel model = new AbstractLPModel();

        model.addVariable("x", -5.0d, 10.0d);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MAXIMIZE);

        model.build();

        LPSolution first = solver.solve(model);

        assertTrue(first.isFeasible());
        assertEquals(10.0d, first.getObjectiveValue(), TOL);

        model.setObjective(
                Map.of("x", 1.0d),
                OptimizationDirection.MINIMIZE);

        model.build();

        LPSolution second = solver.solve(model);

        assertTrue(second.isFeasible());
        assertEquals(-5.0d, second.getObjectiveValue(), TOL);

        assertEquals(10.0d, first.getObjectiveValue(), TOL);
    }
}
