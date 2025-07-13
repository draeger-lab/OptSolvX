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
        model.addVariable("x1", 0.0, 10.0);
        assertFalse(model.isBuilt(), "Model should not be built before calling build()");
        model.build();
        assertTrue(model.isBuilt(), "Model should be built after calling build()");

        // verify variable was added correctly
        assertEquals(1, model.getVariables().size(), "Variable count mismatch");
        Variable v = model.getVariables().get(0);
        assertEquals("x1", v.getName(), "Variable name mismatch");
        assertEquals(0.0, v.getLowerBound(), "Variable lower bound mismatch");
        assertEquals(10.0, v.getUpperBound(), "Variable upper bound mismatch");

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
        model.build();

        // add a constraint and rebuild (build() is idempotent)
        Constraint c = model.addConstraint(
                "c1",
                Map.of("x1", 2.0, "x2", 3.0),
                Constraint.Relation.LEQ,
                10.0
        );

        model.build(); // safe to call again

        // verify constraint was added correctly
        assertEquals(1, model.getConstraints().size(), "Constraint count mismatch");
        Constraint found = model.getConstraints().get(0);
        assertEquals("c1", found.getName(), "Constraint name mismatch");
        assertEquals(Constraint.Relation.LEQ, found.getRelation(), "Constraint relation mismatch");
        assertEquals(10.0, found.getRhs(), "Constraint right-hand side mismatch");

        assertEquals(2.0, found.getCoefficients().get("x1"), "Coefficient for x1 mismatch");
        assertEquals(3.0, found.getCoefficients().get("x2"), "Coefficient for x2 mismatch");
    }
}
