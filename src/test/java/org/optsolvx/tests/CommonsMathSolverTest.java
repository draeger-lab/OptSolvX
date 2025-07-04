package org.optsolvx.tests;

import org.optsolvx.solver.*;
import org.apache.commons.math3.optim.linear.Relationship;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommonsMathSolverTest {

    /**
     * Test cases adapted and inspired by Apache Commons Math SimplexSolverTest:
     * https://github.com/apache/commons-math/tree/master
     */

    @Test
    void testSimplexMaximization() {
        // Test: maximize 3x + 5y
        // Constraints: 2x + y <= 6, x + y <= 4
        // Expected solution: x = 2, y = 2, objective 16
        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(2);
        solver.setObjective(new double[]{3, 5}, true);
        solver.addConstraint(new double[]{2, 1}, Relationship.LEQ, 6);
        solver.addConstraint(new double[]{1, 1}, Relationship.LEQ, 4);

        LPSolution sol = solver.solve();

        assertTrue(sol.feasible);
        assertEquals(0.0, sol.variableValues[0], 1e-6);
        assertEquals(4.0, sol.variableValues[1], 1e-6);
        assertEquals(20.0, sol.objectiveValue, 1e-6);
    }

    @Test
    void testSimplexMinimization() {
        // Test: minimize x + y
        // Constraints: x >= 1, y >= 2
        // Expected solution: x = 1, y = 2, objective = 3

        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(2);
        solver.setObjective(new double[]{1, 1}, false);
        solver.addConstraint(new double[]{1, 0}, Relationship.GEQ, 1);
        solver.addConstraint(new double[]{0, 1}, Relationship.GEQ, 2);

        LPSolution sol = solver.solve();

        assertTrue(sol.feasible);
        assertEquals(1.0, sol.variableValues[0], 1e-6); // x
        assertEquals(2.0, sol.variableValues[1], 1e-6); // y
        assertEquals(3.0, sol.objectiveValue, 1e-6);
    }

    @Test
    void testVariableBounds() {
        // Test: maximize x + y
        // Bounds: 0 <= x <= 1, 1 <= y <= 2
        // Constraints: x + y <= 2.5
        // Expected solution: x = 1, y = 1.5, objective = 2.5

        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(2);
        solver.setObjective(new double[]{1, 1}, true);
        solver.addConstraint(new double[]{1, 1}, Relationship.LEQ, 2.5);
        solver.setVariableBounds(0, 0, 1);   // x in [0,1]
        solver.setVariableBounds(1, 1, 2);   // y in [1,2]

        LPSolution sol = solver.solve();

        assertTrue(sol.feasible);
        assertEquals(1.0, sol.variableValues[0], 1e-6);   // x
        assertEquals(1.5, sol.variableValues[1], 1e-6);   // y
        assertEquals(2.5, sol.objectiveValue, 1e-6);
    }

    @Test
    void testUnsolvableLP() {
        // Test: x >= 2, x <= 1 (no solution)
        // Expected: feasible = false

        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(1);
        solver.setObjective(new double[]{1}, true);
        solver.addConstraint(new double[]{1}, Relationship.GEQ, 2);
        solver.addConstraint(new double[]{1}, Relationship.LEQ, 1);

        LPSolution sol = solver.solve();

        assertFalse(sol.feasible);
    }

    @Test
    void testMaximumFlowLikeLP () {
        // Test: maximize 7x1 + 3x2
        // Bounds: x_i >= 0 (i = 1..4)
        // Constraints:
        // 3x1 - 5x3 <= 0
        // 2x1 - 5x4 <= 0
        // 3x2 - 5x4 <= 0
        // x1 <= 1
        // x2 <= 1
        // Expected solution: objective = 10.0

        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(4); // 4 variables: x1, x2, x3, x4
        solver.setObjective(new double[]{7, 3, 0, 0}, true);

        // Add constraints
        solver.addConstraint(new double[]{3, 0, -5, 0}, Relationship.LEQ, 0.0);
        solver.addConstraint(new double[]{2, 0, 0, -5}, Relationship.LEQ, 0.0);
        solver.addConstraint(new double[]{0, 3, 0, -5}, Relationship.LEQ, 0.0);
        solver.addConstraint(new double[]{1, 0, 0, 0}, Relationship.LEQ, 1.0);
        solver.addConstraint(new double[]{0, 1, 0, 0}, Relationship.LEQ, 1.0);

        // Set non-negativity bounds for all variables
        for (int i = 0; i < 4; i++) {
            solver.setVariableBounds(i, 0.0, Double.POSITIVE_INFINITY);
        }

        //Solve the LP
        LPSolution sol = solver.solve();

        // Assert feasibility and objective value
        assertTrue(sol.feasible, "Solution should be feasible");
        assertEquals(10.0, sol.objectiveValue, 1e-6);
    }

    @Test
    void testUnboundedLP() {
        // Test: maximize x + y
        // Constraints: x - 2y <= -1, x >= 0, y >= 0
        // Expected: unbounded (Solver should throw an exception or set feasible = false)

        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(2);
        solver.setObjective(new double[]{1, 1}, true);
        solver.addConstraint(new double[]{1, -2}, Relationship.LEQ, -1);
        solver.setVariableBounds(0, 0, Double.POSITIVE_INFINITY);
        solver.setVariableBounds(1, 0, Double.POSITIVE_INFINITY);

        boolean unbounded = false;
        try {
            LPSolution sol = solver.solve();
            if (!sol.feasible || Double.isNaN(sol.variableValues[0])) {
                unbounded = true;
            }
        } catch (Exception e) {
            unbounded = true;
        }
        assertTrue(unbounded, "Solver should detect unbounded LP");
    }

    @Test
    void testEqualityConstraing() {
        // Test: maximize x + y
        // Constraints: x + y = 5, x >= 0, y >= 0
        // Expected solution: x = 5, y = 0 (or x = 0, y = 5), obj = 5

        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(2);
        solver.setObjective(new double[]{1, 1}, true);
        solver.addConstraint(new double[]{1, 1}, Relationship.EQ, 5);
        solver.setVariableBounds(0, 0, Double.POSITIVE_INFINITY);
        solver.setVariableBounds(1, 0, Double.POSITIVE_INFINITY);

        LPSolution sol = solver.solve();

        double obj = sol.objectiveValue;
        assertTrue(sol.feasible);
        assertEquals(5.0, obj, 1e-6);
        assertTrue(
                (Math.abs(sol.variableValues[0] - 5.0) < 1e-6 && Math.abs(sol.variableValues[1]) < 1e-6)
                        || (Math.abs(sol.variableValues[1] - 5.0) < 1e-6 && Math.abs(sol.variableValues[0]) < 1e-6),
                "Either x=5, y=0 or x=0, y=5 is a valid solution"
        );

    }

    @Test
    void testNumericalPrecision() {
        // Test: maximize x
        // Constraints: 3x = 1, x >= 0
        // Expected solution: x = 1/3, objective = 1/3
        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(1);
        solver.setObjective(new double[]{1}, true);
        solver.addConstraint(new double[]{3}, Relationship.EQ, 1);
        solver.setVariableBounds(0, 0, Double.POSITIVE_INFINITY);

        LPSolution sol = solver.solve();

        assertTrue(sol.feasible);
        assertEquals(1.0 / 3.0, sol.variableValues[0], 1e-8, "x should be close to 1/3");
        assertEquals(1.0 / 3.0, sol.objectiveValue, 1e-8, "Objective should be close to 1/3");
    }





















}