package org.optsolvx.solver;

import org.optsolvx.model.AbstractLPModel;

public interface LPSolverAdapter {
    LPSolution solve(AbstractLPModel model);
}
