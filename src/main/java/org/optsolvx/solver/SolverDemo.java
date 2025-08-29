package org.optsolvx.solver;

import org.optsolvx.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class SolverDemo {

    public static void main(String[] args) {
        // 1) Build the model
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, Double.POSITIVE_INFINITY);
        model.addVariable("y", 0.0d, Double.POSITIVE_INFINITY);

        // Objective: max x + y  (Java 8 friendly: no Map.of)
        Map<String, Double> obj = new LinkedHashMap<>();
        obj.put("x", 1.0d);
        obj.put("y", 1.0d);
        model.setObjective(obj, OptimizationDirection.MAXIMIZE);

        // Constraints
        Map<String, Double> c1 = new LinkedHashMap<>();
        c1.put("x", 1.0d);
        c1.put("y", 2.0d);
        model.addConstraint("c1", c1, Constraint.Relation.LEQ, 4.0d);

        Map<String, Double> c2 = new LinkedHashMap<>();
        c2.put("x", 1.0d);
        model.addConstraint("c2", c2, Constraint.Relation.LEQ, 3.0d);

        // 2) Finalize and solve
        model.build();

        CommonsMathSolver solver = new CommonsMathSolver();
        LPSolution solution = solver.solve(model);

        // 3) Output
        System.out.println("Variable values: " + solution.getVariableValues());
        System.out.println("Objective: " + solution.getObjectiveValue());
        System.out.println("Feasible: " + solution.isFeasible());
    }
}
