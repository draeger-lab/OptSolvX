package org.optsolvx.tests.lp.ojalgo;

import org.optsolvx.solver.LPSolverAdapter;
import org.optsolvx.backend.ojalgo.OjAlgoSolver;
import org.optsolvx.tests.lp.BaseLPSolverTest;

public class OjAlgoSolverTest extends BaseLPSolverTest {
    @Override
    protected LPSolverAdapter getSolver() {
        return new OjAlgoSolver();
    }
}