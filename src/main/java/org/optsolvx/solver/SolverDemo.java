package org.optsolvx.solver;

import org.apache.commons.math3.optim.linear.Relationship;

public class SolverDemo {
    public static void main(String[] args) {
        ILPSolver solver = new CommonsMathSolver();
        solver.initializeModel(2);
        solver.setObjective(new double[]{3, 5}, true); // max 3x + 5y
        solver.addConstraint(new double[]{2, 1}, Relationship.LEQ, 6); // 2x + y <= 6
        solver.addConstraint(new double[]{1, 1}, Relationship.LEQ, 4); // x + y <= 4

        LPSolution sol = solver.solve();

        if (sol.feasible) {
            System.out.println("x = " + sol.variableValues[0]);
            System.out.println("y = " + sol.variableValues[1]);
            System.out.println("Obj = " + sol.objectiveValue);
        } else {
            System.out.println("Keine Lösung gefunden!");
        }
    }
}