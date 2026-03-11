package org.optsolvx.solver;

import org.optsolvx.model.AbstractLPModel;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.Properties;

/**
 * Global and per-process configuration for selecting an LP solver by name.
 * Resolution priority:
 * 1) explicit override (API argument)
 * 2) per-model preference (AbstractLPModel#getPreferredSolver)
 * 3) Java system property    (-Doptsolvx.solver=ojalgo)
 * 4) Environment variable    (OPTSOLVX_SOLVER=ojalgo)
 * 5) User config file        ($HOME/.optsolvx/config.properties, key=solver)
 * 6) fallback                ("commons-math")
 */
public final class OptSolvXConfig {

    /**
     * Java system property key for solver selection (e.g., -Doptsolvx.solver=ojalgo).
     */
    public static final String PROP = "optsolvx.solver";

    /**
     * Environment variable for solver selection (e.g., export OPTSOLVX_SOLVER=ojalgo).
     */
    public static final String ENV = "OPTSOLVX_SOLVER";

    /**
     * User-level config file (home-relative): contains 'solver=<name>'.
     */
    public static final String FILE = ".optsolvx/config.properties";

    /**
     * Cached global choice; initialized lazily by {@link #getGlobalSolver()}.
     */
    private static volatile String globalSolver = null;

    /**
     * Sets the process-wide solver choice (e.g., from a settings UI).
     */
    public static void setGlobalSolver(String name) {
        globalSolver = name;
    }

    /**
     * Returns the current process-wide solver name. The first call lazily resolves
     * from system property, environment, user config file, then falls back.
     */
    public static String getGlobalSolver() {
        if (globalSolver != null) return globalSolver;

        // 1) Java system property
        String v = System.getProperty(PROP);
        if (v != null && !v.isEmpty()) return globalSolver = v;

        // 2) Environment variable
        v = System.getenv(ENV);
        if (v != null && !v.isEmpty()) return globalSolver = v;

        // 3) User config file: $HOME/.optsolvx/config.properties
        try {
            Path p = Paths.get(System.getProperty("user.home"), FILE);
            if (Files.isRegularFile(p)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(p)) {
                    props.load(in);
                }
                v = props.getProperty("solver");
                if (v != null && !v.isEmpty()) return globalSolver = v;
            }
        } catch (Exception ignored) {
        }

        // 4) Fallback
        return globalSolver = "commons-math";
    }

    /**
     * Resolves an {@link LPSolverAdapter} according to the documented priority:
     * explicitOverride > model preference > global setting > fallback.
     */
    public static LPSolverAdapter resolve(AbstractLPModel model, String explicitOverride) {
        if (explicitOverride != null && !explicitOverride.isEmpty()) {
            return SolverRegistry.create(explicitOverride);
        }
        if (model != null && model.getPreferredSolver() != null && !model.getPreferredSolver().isEmpty()) {
            return SolverRegistry.create(model.getPreferredSolver());
        }
        String global = getGlobalSolver();
        if (SolverRegistry.has(global)) return SolverRegistry.create(global);
        return SolverRegistry.create("commons-math");
    }

    private OptSolvXConfig() {
    }
}
