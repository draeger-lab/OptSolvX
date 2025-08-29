package org.optsolvx.solver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class SolverRegistry {
    private static final Map<String, Supplier<LPSolverAdapter>> SUPPLIERS = new ConcurrentHashMap<>();

    static {
        // Built-ins
        register("commons-math", CommonsMathSolver::new);
        register("ojalgo", OjAlgoSolver::new);

        // Example aliases
        registerAlias("commonsmath", "commons-math");
        registerAlias("cm", "commons-math");
        registerAlias("oj", "ojalgo");
    }

    private static String norm(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    public static void register(String name, Supplier<LPSolverAdapter> supplier) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(supplier, "supplier");
        SUPPLIERS.put(norm(name), supplier);
    }

    /**
     * Optional helper to map an alias to an existing canonical name.
     */
    public static void registerAlias(String alias, String canonical) {
        Objects.requireNonNull(alias, "alias");
        Objects.requireNonNull(canonical, "canonical");
        Supplier<LPSolverAdapter> s = SUPPLIERS.get(norm(canonical));
        if (s == null) throw new IllegalArgumentException("Unknown canonical solver: " + canonical);
        SUPPLIERS.put(norm(alias), s);
    }

    public static boolean has(String name) {
        String n = norm(name);
        return n != null && SUPPLIERS.containsKey(n);
    }

    public static LPSolverAdapter create(String name) {
        String n = norm(name);
        if (n == null || n.isEmpty()) {
            throw new IllegalArgumentException("Solver name must not be null/empty. Known: " + SUPPLIERS.keySet());
        }
        Supplier<LPSolverAdapter> s = SUPPLIERS.get(n);
        if (s == null) {
            throw new IllegalArgumentException("Unknown solver: " + name + " (known: " + SUPPLIERS.keySet() + ")");
        }
        return s.get();
    }

    public static Set<String> names() {
        return Collections.unmodifiableSet(SUPPLIERS.keySet());
    }

    private SolverRegistry() {
    }
}
