package org.optsolvx.solver;

import org.optsolvx.model.*;
import java.util.Map;

public class SolverDemo {
    public static void main(String[] args) {
        // 1. Build the model
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0, Double.POSITIVE_INFINITY);
        model.addVariable("y", 0, Double.POSITIVE_INFINITY);

        // Goal: max x + y
        model.setObjective(Map.of("x", 1.0d, "y", 1.0d), OptimizationDirection.MAXIMIZE);

        // Constraints:
        model.addConstraint("c1", Map.of("x", 1.0d, "y", 2.0d), Constraint.Relation.LEQ, 4.0d);
        model.addConstraint("c2", Map.of("x", 1.0d), Constraint.Relation.LEQ, 3.0d);

        // Finalize model
        model.build();

        // 2. Use the solver
        CommonsMathSolver solver = new CommonsMathSolver();
        LPSolution solution = solver.solve(model);

        // 3. Output result
        System.out.println("Variable values: " + solution.getVariableValues());
        System.out.println("Objective: " + solution.getObjectiveValue());
        System.out.println("Feasible: " + solution.isFeasible());
    }
}
