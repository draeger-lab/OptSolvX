package org.apache.commons.math3.optim.linear;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;

/**
 * Java 8-compatible fused multiply-add operations.
 */
final class FusedArithmetic {

    private static final MethodHandle NATIVE_FMA =
            findNativeFma();

    private FusedArithmetic() {
    }

    private static MethodHandle findNativeFma() {
        try {
            return MethodHandles.publicLookup().findStatic(
                    Math.class,
                    "fma",
                    MethodType.methodType(
                            double.class,
                            double.class,
                            double.class,
                            double.class));

        } catch (NoSuchMethodException ex) {
            return null;

        } catch (IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static double fma(double a, double b, double c) {

        if (NATIVE_FMA == null) {
            return softwareFma(a, b, c);
        }

        try {
            return (double) NATIVE_FMA.invokeExact(a, b, c);

        } catch (RuntimeException | Error ex) {
            throw ex;

        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    static double softwareFma(double a, double b, double c) {

        // Preserve IEEE 754 special-value semantics.
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return a * b + c;
        }

        if (!Double.isFinite(c)) {
            return c;
        }

        if (a == 0.0d || b == 0.0d) {
            return a * b + c;
        }

        // Perform exact arithmetic before rounding to double.
        return new BigDecimal(a)
                .multiply(new BigDecimal(b))
                .add(new BigDecimal(c))
                .doubleValue();
    }
}