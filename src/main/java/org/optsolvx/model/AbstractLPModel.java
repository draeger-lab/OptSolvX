package org.optsolvx.model;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import org.optsolvx.model.OptimizationDirection;

/**
 * Generic base class for linear programming models (LP).
 * Stores all variables, constraints, and the objective function.
 * Can be used independently of any solver backend.
 */
public class AbstractLPModel {

    private static final Logger LOGGER = Logger.getLogger(AbstractLPModel.class.getName());
    /**
     * If true, enables detailed debug logging for model operations.
     * Default: false (no logging).
     */
    private boolean debug = false;

    // List of all variables in the model, in insertion order
    private final List<Variable> variables = new ArrayList<>();

    // List of all constraints in the model
    private final List<Constraint> constraints = new ArrayList<>();

    // Maps variable names to their index in the variables list
    private final Map<String, Integer> variableIndices = new HashMap<>();

    // Maps constraint names to their index in the constraints list
    private final Map<String, Integer> constraintIndices = new HashMap<>();

    // Coefficients of the objective function: varName -> coefficient
    private final Map<String, Double> objectiveCoefficients = new HashMap<>();

    // The optimization direction of the model (MAXIMIZE or MINIMIZE). Default ist Maximize.
    private OptimizationDirection direction = OptimizationDirection.MAXIMIZE;

    // True after build() is called; no further changes allowed
    private boolean built = false;

    private void beforeModelChange() {
        if (built) {
            built = false;
            if (debug) LOGGER.warning(
                    MessageFormat.format(
                            "{0}: Model was changed after build(); 'built' status reset. " +
                                    "Please call build() again before solving.",
                            getClass().getSimpleName()
                    )
            );
        }
    }

    /**
     * Adds a new variable to the model.
     *
     * @param name unique name of the variable
     * @param lower lower bound (inclusive)
     * @param upper upper bound (inclusive)
     * @return index of the variable in the variables list
     * @throws IllegalArgumentException if the name already exists
     * @throws IllegalStateException if the model is already built
     */
    public int addVariable(String name, double lower, double upper) {
        beforeModelChange();
        if (variableIndices.containsKey(name)) {
            if (debug) LOGGER.warning("Duplicate variable name: " + name);
            throw new IllegalArgumentException("Variable name already exists: " + name);
        }
        if (debug) LOGGER.info(MessageFormat.format(
                "{0}: Added variable: {1} [{2,number,0.####}, {3,number,0.####}]",
                getClass().getSimpleName(), name, lower, upper
        ));
        Variable var = new Variable(name, lower, upper);
        int idx = variables.size();
        variables.add(var);
        variableIndices.put(name, idx);
        return idx;
    }

    /**
     * Adds a new linear constraint to the model.
     *
     * @param name unique name of the constraint
     * @param coeffs map of variable name to coefficient in the constraint
     * @param rel type of constraint (LEQ, GEQ, EQ)
     * @param rhs right-hand side value of the constraint
     * @return the new Constraint object
     * @throws IllegalArgumentException if the name already exists
     * @throws IllegalStateException if the model is already built
     */
    public Constraint addConstraint(String name, Map<String, Double> coeffs, Constraint.Relation rel, double rhs) {
        beforeModelChange();
        if (constraintIndices.containsKey(name)) {
            if (debug) LOGGER.warning("Duplicate constraint name: " + name);
            throw new IllegalArgumentException("Constraint name already exists: " + name);
        }
        if (debug) LOGGER.info(MessageFormat.format(
                "{0}: Added constraint {1} ({2}) rhs={3, number,0.####}, vars={4}",
                getClass().getSimpleName(), name, rel, rhs, coeffs.keySet()
        ));
        Constraint c = new Constraint(name, coeffs, rel, rhs);
        int idx = constraints.size();
        constraints.add(c);
        constraintIndices.put(name, idx);
        return c;
    }

    /**
     * Sets the objective function for the model.
     *
     * @param coeffs map of variable name to objective coefficient
     * @param direction the optimization direction (MAXIMIZE or MINIMIZE)
     * @throws IllegalStateException if the model is already built
     */
    public void setObjective(Map<String, Double> coeffs, OptimizationDirection direction) {
        beforeModelChange();
        objectiveCoefficients.clear();
        objectiveCoefficients.putAll(coeffs);
        this.direction = direction;
    }

    /**
     * Finalizes the model, assigns indices to variables and constraints.
     * After calling build(), no further variables or constraints can be added
     * until the model is changed again. If the model is changed after build(),
     * the 'built' flag will be reset and build() must be called again before solving.
     * <p>
     * Logs a summary when the model is finalized.
     * </p>
     */
    public void build() {
        if (built) return;
        if (debug) LOGGER.info(MessageFormat.format(
                "{0}: Building model with {1} variables and {2} constraints.",
                getClass().getSimpleName(), variables.size(), constraints.size()
        ));
        built = true;
        if (debug) LOGGER.info(MessageFormat.format(
                "{0}: Model finalized. No further modifications allowed.",
                getClass().getSimpleName()
        ));
    }

    /**
     * @return an unmodifiable list of all variables
     */
    public List<Variable> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    /**
     * Returns an unmodifiable list of all constraints.
     */
    public List<Constraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    /**
     * Returns an unmodifiable map of the objective function coefficients.
     */
    public Map<String, Double> getObjectiveCoefficients() {
        return Collections.unmodifiableMap(objectiveCoefficients);
    }

    /**
     * Returns the current optimization direction (MAXIMIZE or MINIMIZE).
     * @return the optimization direction
     */
    public OptimizationDirection getDirection() {
        return direction;
    }

    /**
     * Sets the optimization direction for this model.
     * @param direction the optimization direction (MAXIMIZE or MINIMIZE)
     */
    public void setDirection(OptimizationDirection direction) {
        beforeModelChange();
        this.direction = direction;
    }

    /**
     * @return true if build() has been called and the model is finalized
     */
    public boolean isBuilt() {
        return built;
    }

    /**
     * Returns the index of a variable by its name.
     * @param name the variable name
     * @return index of the variable in the model
     * @throws IllegalArgumentException if not found
     */
    public int getVariableIndex(String name) {
        Integer idx = variableIndices.get(name);
        if (idx == null) throw new IllegalArgumentException("No such variable: " + name);
        return idx;
    }

    /**
     * Returns the index of a constraint by its name.
     * @param name the constraint name
     * @return index of the constraint in the model
     * @throws IllegalArgumentException if not found
     */
    public int getConstraintIndex(String name) {
        Integer idx = constraintIndices.get(name);
        if (idx == null) throw new IllegalArgumentException("No such constraint: " + name);
        return idx;
    }


    /**
     * Returns a human-readable string of the model for debugging.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(":\n");
        sb.append("Variables:\n");
        for (Variable v : variables) sb.append("  ").append(v).append("\n");
        sb.append("Constraints:\n");
        for (Constraint c : constraints) sb.append("  ").append(c).append("\n");
        sb.append("Objective: ").append(objectiveCoefficients)
                .append(" direction=").append(direction).append("\n");
        return sb.toString();
    }

    /**
     * Enable or disable debug logging for this model.
     * Logging is OFF by default.
     * Call setDebug(true) before model building to activate.
     * Example: model.setDebug(true);   // Logging on
     *          model.setDebug(false);  // Logging off
     *
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    protected void doBuild() { /* nothing here for base class */ }
}
