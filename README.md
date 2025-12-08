# OptSolvX

[![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=plastic)](http://opensource.org/licenses/MIT)
![Code Size](https://img.shields.io/github/languages/code-size/draeger-lab/OptSolvX.svg?style=plastic)
![Downloads of all releases](https://img.shields.io/github/downloads/draeger-lab/OptSolvX/total.svg?style=plastic)

OptSolvX is a flexible Java library for solving linear programming (LP) problems with multiple interchangeable solver
backends.  
It provides a clean, test-driven API for building, comparing and extending LP solvers.  
OptSolvX is intended for applications in mathematics, research, and systems biology.


► Features
----------------------------

- Solver-agnostic LP core: `AbstractLPModel`, `Variable`, `Constraint`, `OptimizationDirection`, `LPSolution`
- Unified solver adapter interface: `LPSolverAdapter`
- Pluggable backends: `CommonsMathSolver`, `OjAlgoSolver` (GLPK, CBC, SCIP, CPLEX, Gurobi planned)
- Test-driven development with JUnit 5
- Clean logging & validation (build checks, bounds, relations)
- Easy to extend with custom backends; demo included

► Status
----------------------------

- LP modeling & solving: maximize/minimize, EQ/LEQ/GEQ constraints, variable bounds, build() workflow
- Backends: Commons Math and ojAlgo adapters ready (`CommonsMathSolver`, `OjAlgoSolver`)
- Builds: Java 22 by default; optional Java 8 bytecode via compat8 profile (classifier jdk8)

► Installation
----------------------------

Requirements: Maven ≥ 3.9, Java 22 or newer (for building).

Optional: build an additional Java 8 bytecode artifact via profile compat8.

```bash
git clone https://github.com/draeger-lab/OptSolvX.git
cd OptSolvX
```

Default (Java 22) - installs to local Maven repo

```bash
mvn clean install
```

Optional: Java 8 bytecode JAR (classifier jdk8)

```bash
mvn -P compat8 -DskipTests clean package
```

Artifacts

- target/optsolvx-<version>.jar - Java 22 (default)
- target/optsolvx-<version>-jdk8.jar - Java 8 bytecode (compatibility)

► Java Version
----------------------------

OptSolvX requires **Java 22 or newer** to build the library.  
The build enforces this via the Maven Enforcer plugin.

If a different JDK is active, the build will fail early with a clear message.  
Optional: use the `compat8` profile to produce a Java 8 bytecode JAR for downstream projects.


► Testing
----------------------------

All mathematical LP tests are found in `src/test/java/org/optsolvx/tests/lp`.

Biological and advanced tests (e.g., with SBML) will be added in the future under `src/test/java/org/optsolvx/bio`.

Run all tests with:

```bash
mvn test
```

► Quick Demo
----------------------------

Run the built-in demo (max x + y with two constraints) using the OptSolvX backend-selection mechanism.

**From IDE:** run `org.optsolvx.solver.SolverDemo`.

**From Maven (CLI):**

```bash
mvn -q exec:java
# If needed:
# mvn -q -DskipTests exec:java -Dexec.mainClass=org.optsolvx.solver.SolverDemo
```

By default, the solver is chosen via `OptSolvXConfig` using:
- CLI override (first argument)
- Per-model preference (`model.setPreferredSolver(...)`)
- User config (`~/.optsolvx/config.properties`)
- Environment variable `OPTSOLVX_SOLVER`
- Built-in default (`commons-math`)

You can explicitly select a backend like this:

```bash
mvn -q exec:java -Dexec.args="ojalgo"
```

Expected Output for the demo problem:

```text
Variable values: {x=3.0, y=0.5}
Objective: 3.5
Feasible:  true
```

***Optional debug:*** enable verbose model logging in the demo:

```java
model.setDebug(true); // call before model.build()
```

► Contribution
----------------------------

Contributions and feedback are welcome! Please open issues or pull requests on this epository.