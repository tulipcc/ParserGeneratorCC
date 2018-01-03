# ParserGeneratorCC

Fork of JavaCC 7.0.3 - more to come

# Differences to JavaCC

The overall goal is to maintain compatibility to JavaCC but
* The JavaCC code itself should be better maintainable
* The JavaCC code itself should conform to best-practises
* Because this is NOT JavaCC the class names are similar, but the base package name changed from `net.javacc` to `com.helger.pgcc`  
 
## Incompatible changes

* The JavaCC option `GENERATE_STRING_BUILDER` was removed - it was never evaluated
* The JavaCC option `LEGACY_EXCEPTION_HANDLING` was removed - that was too much 1990 ;) - see issue #7
* The JavaCC option `GENERATE_CHAINED_EXCEPTION` was replaced with deduction from the Java version (&ge; 1.4)
* The JavaCC option `GENERATE_GENERICS` was replaced with deduction from the Java version (&ge; 1.5)
* The JavaCC option `GENERATE_ANNOTATIONS` was replaced with deduction from the Java version (&ge; 1.5)
