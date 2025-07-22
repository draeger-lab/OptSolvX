package org.optsolvx.tests.lp;

import org.optsolvx.solver.LPSolverAdapter;
import org.optsolvx.solver.CommonsMathSolver;

public class CommonsMathSolverTest extends BaseLPSolverTest{
    @Override
    protected LPSolverAdapter getSolver() {
        return new CommonsMathSolver();
    }
}
