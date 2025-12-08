package org.optsolvx.solver;

import org.optsolvx.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SolverDemo {

    public static void main(String[] args) {
        // 1) Build a simple LP model: max x + y subject to two constraints.
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, Double.POSITIVE_INFINITY);
        model.addVariable("y", 0.0d, Double.POSITIVE_INFINITY);

        // Objective: max x + y  (Java 8 friendly: no Map.of)
        Map<String, Double> obj = new LinkedHashMap<String, Double>();
        obj.put("x", 1.0d);
        obj.put("y", 1.0d);
        model.setObjective(obj, OptimizationDirection.MAXIMIZE);

        // c1: x + 2y <= 4
        Map<String, Double> c1 = new LinkedHashMap<>();
        c1.put("x", 1.0d);
        c1.put("y", 2.0d);
        model.addConstraint("c1", c1, Constraint.Relation.LEQ, 4.0d);

        // c2: x <= 3
        Map<String, Double> c2 = new LinkedHashMap<>();
        c2.put("x", 1.0d);
        model.addConstraint("c2", c2, Constraint.Relation.LEQ, 3.0d);

        // 2) Finalize model
        model.build();

        // Optional explicit solver name via first CLI argument (highest priority).
        // Example: mvn -q exec:java -Dexec.args="ojalgo"
        String explicitName = null;
        if (args != null && args.length > 0 && args[0] != null) {
            String candidate = args[0].trim();
            if (!candidate.isEmpty()) {
                explicitName = candidate;
            }
        }

        // Resolve backend via OptSolvXConfig (explicit > model > global > fallback).
        LPSolverAdapter backend = OptSolvXConfig.resolve(model, explicitName);

        // 5) Solve and print result
        LPSolution solution = backend.solve(model);

        // 6) Output
        System.out.println("Backend:  " + backend.getClass().getSimpleName());
        System.out.println("Vars:     " + solution.getVariableValues());
        System.out.println("Objective:" + solution.getObjectiveValue());
        System.out.println("Feasible: " + solution.isFeasible());
    }
}