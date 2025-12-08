package org.optsolvx.tests.lp;

import org.optsolvx.solver.LPSolverAdapter;
import org.optsolvx.solver.OjAlgoSolver;

public class OjAlgoSolverTest extends BaseLPSolverTest {
    @Override
    protected LPSolverAdapter getSolver() {
        return new OjAlgoSolver();
    }
}