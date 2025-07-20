import org.optsolvx.model.*;
import org.optsolvx.solver.CommonsMathSolver;
import org.optsolvx.solver.LPSolution;

import java.util.Map;

public class SolverDemo {
    public static void main(String[] args) {
        // 1. Modell aufbauen
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0, Double.POSITIVE_INFINITY);
        model.addVariable("y", 0, Double.POSITIVE_INFINITY);

        // Ziel: max x + y
        model.setObjective(Map.of("x", 1.0d, "y", 1.0d), OptimizationDirection.MAXIMIZE);

        // Constraints:
        model.addConstraint("c1", Map.of("x", 1.0d, "y", 2.0d), Constraint.Relation.LEQ, 4.0d);
        model.addConstraint("c2", Map.of("x", 1.0d), Constraint.Relation.LEQ, 3.0d);

        // Modell finalisieren
        model.build();

        // 2. Solver benutzen
        CommonsMathSolver solver = new CommonsMathSolver();
        LPSolution solution = solver.solve(model);

        // 3. Ergebnis ausgeben
        System.out.println("Variable values: " + solution.getVariableValues());
        System.out.println("Objective: " + solution.getObjectiveValue());
        System.out.println("Feasible: " + solution.isFeasible());
    }
}
