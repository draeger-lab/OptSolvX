package org.optsolvx.solver;

import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.MaxIter;
import org.optsolvx.model.*;

import java.util.*;

public class CommonsMathSolver {

    public LPSolution solve(AbstractLPModel model) {
        if (model == null || !model.isBuilt()) {
            throw new IllegalArgumentException("Model is null or not built.");
        }

        List<Variable> variables = model.getVariables();
        int numVariables = variables.size();

        // Objective coefficients, mapped by variable index
        double[] objectiveCoefficients = new double[numVariables];
        Map<String, Double> ojectiveCoefficientsMap = model.getObjectiveCoefficients();
        for (int variableIndex = 0; variableIndex < numVariables; variableIndex++) {
            String name = variables.get(variableIndex).getName();
            objectiveCoefficients[variableIndex] = ojectiveCoefficientsMap.getOrDefault(name, 0.0d);
        }

        // Bounds
        double[] lowerBounds = new double[numVariables];
        double[] upperBounds = new double[numVariables];
        for (int variableIndex = 0; variableIndex < numVariables; variableIndex++) {
            lowerBounds[variableIndex] = variables.get(variableIndex).getLowerBound();
            upperBounds[variableIndex] = variables.get(variableIndex).getUpperBound();
        }

        // Constraints
        List<LinearConstraint> allConstraints = new ArrayList<>();
        for (Constraint constraint : model.getConstraints()) {
            double[] coefficients = new double[numVariables];
            for (Map.Entry<String, Double> entry : constraint.getCoefficients().entrySet()) {
                int variableIndex = model.getVariableIndex(entry.getKey());
                coefficients[variableIndex] = entry.getValue();
            }
            Relationship relationship = toCommonsMathRelationship(constraint.getRelation());
            allConstraints.add(new LinearConstraint(coefficients, relationship, constraint.getRhs()));
        }

        // Variable bounds as constraints
        for (int i = 0; i < numVariables; i++) {
            double[] coefficients = new double[numVariables];
            coefficients[i] = 1.0d;
            allConstraints.add(new LinearConstraint(coefficients, Relationship.GEQ, lowerBounds[i]));
            if (upperBounds[i] < Double.POSITIVE_INFINITY) {
                allConstraints.add(new LinearConstraint(coefficients, Relationship.LEQ, upperBounds[i]));
            }
        }

        LinearObjectiveFunction objective = new LinearObjectiveFunction(objectiveCoefficients, 0);
        SimplexSolver solver = new SimplexSolver();
        GoalType goalType = (model.getDirection() == OptimizationDirection.MAXIMIZE)
                ? GoalType.MAXIMIZE : GoalType.MINIMIZE;

        try {
            PointValuePair result = solver.optimize(
                    new MaxIter(100),
                    objective,
                    new LinearConstraintSet(allConstraints),
                    goalType,
                    new NonNegativeConstraint(true)
            );
            double[] solutionVariables = result.getPoint();
            Map<String, Double> variableMap = new LinkedHashMap<>();
            for (int i = 0; i < numVariables; i++) {
                variableMap.put(variables.get(i).getName(), solutionVariables[i]);
            }
            double objectiveValue = result.getValue();
            return new LPSolution(variableMap, objectiveValue, true);
        } catch (Exception e) {
            return new LPSolution(Collections.emptyMap(), Double.NaN, false);
        }
    }

    private Relationship toCommonsMathRelationship(Constraint.Relation relationship) {
        switch (relationship) {
            case LEQ:
                return Relationship.LEQ;
            case GEQ:
                return Relationship.GEQ;
            case EQ:
                return Relationship.EQ;
            default:
                throw new IllegalArgumentException("Unknown relationship: " + relationship);
        }
    }


}
