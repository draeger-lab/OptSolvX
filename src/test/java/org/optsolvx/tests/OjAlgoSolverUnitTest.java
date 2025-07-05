package org.optsolvx.tests;

import org.optsolvx.solver.*;
import org.apache.commons.math3.optim.linear.Relationship;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases adapted and inspired by OjAlgo ExpressionsBasedModelTest:
 * <a href="https://github.com/optimatika/ojAlgo/tree/develop">...</a>
 */
public class OjAlgoSolverUnitTest {

    @Test
    void testSimplexMaximization() {
        // Test: maximize 3x + 5y
        // Constraints: 2x + y <= 6, x + y <= 4
        // Expected solution: x = 2, y = 2, objective 16

        ILPSolver solver = new OjAlgoSolver();
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
        assertEquals(1.0, sol.variableValues[0], 1e-6);
        assertEquals(2.0, sol.variableValues[1], 1e-6);
        assertEquals(3.0, sol.objectiveValue, 1e-6);

    }




}
