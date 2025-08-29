# OptSolvX

<!--

Place for licenses, java links and passings. Inspired and copied from SBSCL and JSBML as an example

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-22-blue?logo=java&logoColor=white)](https://adoptium.net/de/temurin/releases/)
[![Build](https://github.com/draeger-lab/OptSolvX/actions/workflows/ci.yml/badge.svg)](https://github.com/draeger-lab/OptSolvX/actions)

-->

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
- Pluggable backends: CommonsMathSolver (ready), OjAlgoSolver (planned)
- Test-driven development with JUnit 5
- Clean logging & validation (build checks, bounds, relations)
- Easy to extend with custom backends; demo included

► Status
----------------------------

- LP modeling & solving: maximize/minimize, EQ/LEQ/GEQ constraints, variable bounds, build() workflow
- Backends: Commons Math adapter ready; ojAlgo adapter planned
- Builds: Java 22 by default; optional Java 8 bytecode via compat8 profile (classifier jdk8)

► Installation
----------------------------

Requirements: Maven ≥ 3.9, Java 22 (default).

Optional: build an additional Java 8 bytecode artifact via profile compat8.

```
git clone https://github.com/draeger-lab/OptSolvX.git
cd OptSolvX
```

Default (Java 22) - installs to local Maven repo

```
mvn clean install
```

Optional: Java 8 bytecode JAR (classifier jdk8)

```
mvn -P compat8 -DskipTests clean package
```

Artifacts

- target/optsolvx-<version>.jar - Java 22 (default)
- target/optsolvx-<version>-jdk8.jar - Java 8 bytecode (compatibility)

► Testing
----------------------------

All mathematical LP tests are found in `src/test/java/org/optsolvx/tests/lp`.

Biological and advanced tests (e.g., with SBML) will be added in the future under `src/test/java/org/optsolvx/bio`.

Run all tests with:

```
mvn test
```

<!--

► Getting started with OptSolvX
----------------------------

Please see the user manual at .

If you use JSBML, we encourage you to subscribe to or monitor via RSS the [jsbml-development](https://groups.google.com/forum/#!forum/jsbml-development) mailing list/web forum, where people discuss the development and use of JSBML.  Being a member of [jsbml-development](https://groups.google.com/forum/#!forum/jsbml-development) will enable you to keep in touch with the latest developments in JSBML as well as to ask questions and share your experiences with fellow developers and users of JSBML.

-->

► Quick Demo
----------------------------

Run the built-in demo (max x + y with two constraints) using the Commons Math backend.

**From IDE:** run `org.optsolvx.solver.SolverDemo`.

**From Maven (CLI):**

```bash
mvn -q exec:java
# If needed:
# mvn -q -DskipTests exec:java -Dexec.mainClass=org.optsolvx.solver.SolverDemo
```

Expected Output:

```bash
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

Contributions and feedback are welcome! Please open issues or pull requests on GitHub.

---
*State of this README as of: 20.08.25*
