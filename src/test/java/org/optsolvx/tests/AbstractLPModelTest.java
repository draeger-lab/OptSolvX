package org.optsolvx.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.optsolvx.model.AbstractLPModel;
import org.optsolvx.model.Constraint;
import org.optsolvx.model.Variable;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AbstractLPModel}.
 * Verifies addVariable/addConstraint, build()
 * and basic getters/toString behavior.
 */
public class AbstractLPModelTest {

    private AbstractLPModel model;

    @BeforeEach
    void setUp() {
        // Use an anonymous subclass to implement the abstract doBuild()
        model = new AbstractLPModel() {
            @Override
            protected void doBuild() {
                // no extra build steps needed for this test
            }
        };
    }

    @Test
    void testAddVariableAndBuild() {
        // add a variable and verify before/after build()
        model.addVariable("x1", 0.0d, 10.0d);
        assertFalse(model.isBuilt(), "Model should not be built before calling build()");
        model.build();
        assertTrue(model.isBuilt(), "Model should be built after calling build()");

        // verify variable was added correctly
        assertEquals(1, model.getVariables().size(), "Variable count mismatch");
        Variable v = model.getVariables().get(0);
        assertEquals("x1", v.getName(), "Variable name mismatch");
        assertEquals(0.0d, v.getLowerBound(), "Variable lower bound mismatch");
        assertEquals(10.0d, v.getUpperBound(), "Variable upper bound mismatch");

        // toString() contains variable name
        String repr = model.toString();
        assertNotNull(repr, "toString() must not return null");
        assertTrue(repr.contains("x1"), "toString() should include variable name");
    }

    @Test
    void testAddConstraintAndBuild() {
        // prepare two variables and finalize the model
        model.addVariable("x1", 0, 5);
        model.addVariable("x2", 0, 7);

        // add a constraint and rebuild (build() is idempotent)
        Constraint c = model.addConstraint(
                "c1",
                Map.of("x1", 2.0d, "x2", 3.0d),
                Constraint.Relation.LEQ,
                10.0d
        );

        model.build(); // safe to call again

        // verify constraint was added correctly
        assertEquals(1, model.getConstraints().size(), "Constraint count mismatch");
        Constraint found = model.getConstraints().get(0);
        assertEquals("c1", found.getName(), "Constraint name mismatch");
        assertEquals(Constraint.Relation.LEQ, found.getRelation(), "Constraint relation mismatch");
        assertEquals(10.0d, found.getRhs(), "Constraint right-hand side mismatch");

        assertEquals(2.0d, found.getCoefficients().get("x1"), "Coefficient for x1 mismatch");
        assertEquals(3.0d, found.getCoefficients().get("x2"), "Coefficient for x2 mismatch");
    }

    @Test
    void testDuplicateVariableNameThrowsException() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x1", 0, 10);
        // Adding a second variable with the same name should throw
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            model.addVariable("x1", 0, 20);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("x1"));
        assertTrue(ex.getMessage().toLowerCase().contains("variable"));
    }

    @Test
    void testDuplicateConstraintNameThrowsException() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x1", 0, 10);
        model.addConstraint("c1", Map.of("x1", 1.0d), Constraint.Relation.LEQ, 5.0d);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            model.addConstraint("c1", Map.of("x1", 2.0d), Constraint.Relation.GEQ, 3.0d);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("c1"));
        assertTrue(ex.getMessage().toLowerCase().contains("constraint"));
    }

    @Test
    void testAddVariableAfterBuildIsAllowedAndResetsBuiltFlag() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x1", 0, 10);
        model.build();
        assertTrue(model.isBuilt(), "Model should be built after build()");

        // Add variable after build (should not throw)
        model.addVariable("x2", 0, 5);
        assertFalse(model.isBuilt(), "Adding variable after build should reset built to false");
    }

    @Test
    void testAddConstraintAfterBuildIsAllowedAndResetsBuiltFlag() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x1", 0, 10);
        model.build();
        assertTrue(model.isBuilt(), "Model should be built after build()");

        // Add constraint after build (should not throw)
        model.addConstraint("c1", Map.of("x1", 1.0d), Constraint.Relation.LEQ, 5.0d);
        assertFalse(model.isBuilt(), "Adding constraint after build should reset built to false");
    }

    @Test
    void testBuiltFlagResetsAfterModelChange() {
        AbstractLPModel model = new AbstractLPModel();

        //Step 1: Add a variable and build the model
        model.addVariable("x1", 0, 10);
        model.build();
        assertTrue(model.isBuilt(), "After build() was called, built should be true.");

        // Step 2: Add another variable after build() (should reset built to false)
        model.addVariable("x2", 0, 5);
        assertFalse(model.isBuilt(), "After modifying the model, built should be false");

        // Step 3: Call build() again
        model.build();
        assertTrue(model.isBuilt(), "After calling build() again, built should be true.");
    }

    @Test
    void testGetVariableIndexReturnsCorrectIndex() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x1", 0, 10);
        model.addVariable("x2", -5.0d, 5.0d);

        assertEquals(0, model.getVariableIndex("x1"), "Index of x1 should be 0");
        assertEquals(1, model.getVariableIndex("x2"), "Index of x2 should be 1");

        // Negative test: unknown variable should throw
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            model.getVariableIndex("doesNotExist");
        });
        assertTrue(ex.getMessage().toLowerCase().contains("no such variable"));
    }

    @Test
    void testGetConstraintIndexReturnsCorrectIndex() {
        AbstractLPModel model = new AbstractLPModel();
        model.addVariable("x1", 0, 10);
        model.addConstraint("c1", Map.of("x1", 1.0d), Constraint.Relation.LEQ, 5.0d);
        model.addConstraint("c2", Map.of("x1", 2.0d), Constraint.Relation.LEQ, 7.0d);

        assertEquals(0, model.getConstraintIndex("c1"), "Index of c1 should be 0");
        assertEquals(1, model.getConstraintIndex("c2"), "Index of c2 should be 1");

        // Negative test: unknown constraint should throw
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            model.getConstraintIndex("doesNotExist");
        });
        assertTrue(ex.getMessage().toLowerCase().contains("no such constraint"));
    }
}
