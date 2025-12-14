package org.optsolvx.tests.lp.commonsmath;

import org.optsolvx.solver.LPSolverAdapter;
import org.optsolvx.backend.commonsmath.CommonsMathSolver;
import org.optsolvx.tests.lp.BaseLPSolverTest;

public class CommonsMathSolverTest extends BaseLPSolverTest {
    @Override
    protected LPSolverAdapter getSolver() {
        return new CommonsMathSolver();
    }
}
