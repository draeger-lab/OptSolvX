package org.optsolvx.tests.lp;

import org.junit.jupiter.api.Test;
import org.optsolvx.model.AbstractLPModel;
import org.optsolvx.model.OptimizationDirection;
import org.optsolvx.solver.*;

import static org.junit.jupiter.api.Assertions.*;

public class OjAlgoSolverTest {

    @Test
    public void solvesSimpleLP() {
        AbstractLPModel m = new AbstractLPModel()
                .direction(OptimizationDirection.MAXIMIZE)
                .var("x", 0, 10).obj("x", 1.0)
                .var("y", 0, 10).obj("y", 2.0)
                .leq("c1", 1.0, "x", 1.0, "y", 8.0) // x + y <= 8
                .build();

        LPSolverAdapter solver = new OjAlgoSolver();
        LPSolution sol = solver.solve(m);

        assertTrue(sol.isFeasible());
        assertEquals(16.0, sol.getObjectiveValue(), 1e-9); // y=8, x=0
        assertEquals(0.0, sol.getVariableValues().get("x"), 1e-9);
        assertEquals(8.0, sol.getVariableValues().get("y"), 1e-9);
    }
}