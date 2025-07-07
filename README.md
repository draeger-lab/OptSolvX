# OptSolvX

<!--

Place for licenses, java links and passings. Inspired and copied from SBSCL and JSBML as an example

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-22-blue?logo=java&logoColor=white)](https://adoptium.net/de/temurin/releases/)
[![Build](https://github.com/draeger-lab/OptSolvX/actions/workflows/ci.yml/badge.svg)](https://github.com/draeger-lab/OptSolvX/actions)

-->

OptSolvX is a flexible Java library for solving linear programming (LP) problems with multiple interchangeable solver backends.  
It provides a clean, test-driven API for building, comparing and extending LP solvers.  
OptSolvX is intended for applications in mathematics, research, and systems biology.


► Features
----------------------------

- Unified LP solver interface (`ILPSolver`)
- Pluggable solver adapters: `CommonsMathSolver`, `OjAlgoSolver`, and more coming soon
- Test-driven (JUnit5) development
- Easily extendable with new solver backends
- Example/test coverage for common LP scenarios


► Status
----------------------------

- LP modeling and solving (maximize/minimize)
- Constraints and variable bounds supported
- Ready-to-use with Commons Math and OjAlgo adapters

<!-- 
- CI (GitHub Actions) is set up for all builds and tests
-->


► Installation
----------------------------

**Requirements:** Java 22+, Maven 3.9+

```
git clone https://github.com/draeger-lab/OptSolvX.git
cd OptSolvX
mvn clean install
```

► Testing
----------------------------

All mathematical LP tests are found in `src/test/java/org/optsolvx/tests/`.
Biological and advanced tests (e.g., with SBML) will be added in the future under `src/test/java/org/optsolvx/biotests/.

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


► Contribution
----------------------------

Contributions and feedback are welcome! Please open issues or pull requests on GitHub.

---
*State of this README as of: 07.07.25*