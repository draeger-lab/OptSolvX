package org.optsolvx.tests.lp;

import org.optsolvx.model.*;
import org.optsolvx.solver.LPSolution;
import org.junit.jupiter.api.Test;
import org.optsolvx.solver.LPSolverAdapter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseLPSolverTest {
    protected abstract LPSolverAdapter getSolver();

    @Test
    void testSimpleMaximization() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0, 10);
        model.addVariable("y", 0, 10);
        model.setObjective(Map.of("x", 3.0d, "y", 5.0d), OptimizationDirection.MAXIMIZE);
        model.addConstraint("c1", Map.of("x", 2.0d, "y", 1.0d), Constraint.Relation.LEQ, 6.0d);
        model.addConstraint("c2", Map.of("x", 1.0d, "y", 1.0d), Constraint.Relation.LEQ, 4.0d);
        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(0.0d, sol.getVariableValues().get("x"), 1e-6);
        assertEquals(4.0d, sol.getVariableValues().get("y"), 1e-6);
        assertEquals(20.0d, sol.getObjectiveValue(), 1e-6);
    }

    // Space for more tests
}
