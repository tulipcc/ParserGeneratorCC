# ParserGeneratorCC

Fork of JavaCC 7.0.3 because the original code base has poor quality and PRs are not really merged.
The overall goal is to maintain compatibility to JavaCC but
* The code itself should be better maintainable
* The code itself should conform to best-practices
* Because this is NOT JavaCC the class names are similar, but the base package name changed from `net.javacc` to `com.helger.pgcc`  

See https://github.com/phax/ph-javacc-maven-plugin/ for a Maven plugin that uses this CC.

# News and noteworthy

* v1.1.0 - work in progress
  * Focus is on improving the quality and consistency of the template files - this may cause interoperability problem because names change etc.
* v1.0.2 - 2018-01-08
  * Fixed an error in jump-patching that was originally only considered if legacy exception handling was enabled - now ParserGeneratorCC can create itself using itself!
* v1.0.1 - 2018-01-05
  * Initial release with expected output compatibility to JavaCC 7.0.3
  * The JavaCC option `GENERATE_STRING_BUILDER` was removed - it was never evaluated
  * The JavaCC option `LEGACY_EXCEPTION_HANDLING` was removed - that was too much 1990 ;) - see issue #7. This implies that the class `TokenMgrError` is no longer available!
  * The JavaCC option `GENERATE_CHAINED_EXCEPTION` was replaced with deduction from the Java version (&ge; 1.4)
  * The JavaCC option `GENERATE_GENERICS` was replaced with deduction from the Java version (&ge; 1.5)
  * The JavaCC option `GENERATE_ANNOTATIONS` was replaced with deduction from the Java version (&ge; 1.5)
  * The main applications previously located in the root packaged were moved to package `com.helger.pgcc.main`
  * Minor improvements for Java output if JDK level is &ge; 1.7
  * https://github.com/phax/ph-javacc-maven-plugin/ starting from v4 will use this package instead of JavaCC.
  * No pure `RuntimeException` or `Error` classes are thrown inside the code
* v1.0.0 - 2018-01-05 - had a regression and was therefore never released binary

## Maven usage
Add the following to your pom.xml to use this artifact:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>parser-generator-cc</artifactId>
  <version>1.0.2</version>
</dependency>
``` 

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodeingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
