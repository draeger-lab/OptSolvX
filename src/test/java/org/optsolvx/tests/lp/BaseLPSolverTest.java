package org.optsolvx.tests.lp;

import org.optsolvx.model.*;
import org.optsolvx.solver.LPSolution;
import org.junit.jupiter.api.Test;
import org.optsolvx.solver.LPSolverAdapter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseLPSolverTest {
    protected abstract LPSolverAdapter getSolver();

    @Test
    void testSimpleMaximization() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0, 10);
        model.addVariable("y", 0, 10);
        model.setObjective(Map.of("x", 3.0d, "y", 5.0d), OptimizationDirection.MAXIMIZE);
        model.addConstraint("c1", Map.of("x", 2.0d, "y", 1.0d), Constraint.Relation.LEQ, 6.0d);
        model.addConstraint("c2", Map.of("x", 1.0d, "y", 1.0d), Constraint.Relation.LEQ, 4.0d);
        model.build();

        LPSolution sol = getSolver().solve(model);

        assertTrue(sol.isFeasible());
        assertEquals(0.0d, sol.getVariableValues().get("x"), 1e-6);
        assertEquals(4.0d, sol.getVariableValues().get("y"), 1e-6);
        assertEquals(20.0d, sol.getObjectiveValue(), 1e-6);
    }

    @Test
    void testAddVariable() {
        AbstractLPModel model = new AbstractLPModel();
        int idx = model.addVariable("x", 0.0d, 10.0d);
        assertEquals(0, idx);
        assertEquals(1, model.getVariables().size());
        assertEquals("x", model.getVariables().getFirst().getName());
    }

    @Test
    void testAddConstraint() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, 10.0d);
        Constraint c = model.addConstraint("c1", Map.of("x", 1.0d), Constraint.Relation.LEQ, 5.0d);
        assertEquals("c1", c.getName());
        assertEquals(1, model.getConstraints().size());
        assertEquals(c, model.getConstraints().getFirst());
    }

    @Test
    void testBuildSetsBuiltFlag() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, 10.0d);
        assertFalse(model.isBuilt());
        model.build();
        assertTrue(model.isBuilt());
    }

    @Test
    void testAddDuplicateVariableThrows() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, 10.0d);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> model.addVariable("x", 0.0d, 5.0d));
        assertTrue(ex.getMessage().toLowerCase().contains("variable"));
    }

    @Test
    void testAddDuplicateConstraintThrows() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, 10.0d);
        model.addConstraint("c1", Map.of("x", 1.0d), Constraint.Relation.LEQ, 5.0d);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> model.addConstraint("c1", Map.of("x", 2.0d), Constraint.Relation.GEQ, 7.0d));
        assertTrue(ex.getMessage().toLowerCase().contains("constraint"));
    }

    @Test
    void testSetDebugEnablesLogging() {
        AbstractLPModel model = new AbstractLPModel();
        model.setDebug(true);
    }

    @Test
    void testSetDirection() {
        AbstractLPModel model = new AbstractLPModel();
        model.setDirection(OptimizationDirection.MINIMIZE);
        assertEquals(OptimizationDirection.MINIMIZE, model.getDirection());
    }

    @Test
    void testModelChangeAfterBuildResetsFlag() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x", 0.0d, 10.0d);
        model.build();
        assertTrue(model.isBuilt());
        model.addVariable("y", 0.0d, 5.0d); // should reset built-Flag
        assertFalse(model.isBuilt());
    }
}
