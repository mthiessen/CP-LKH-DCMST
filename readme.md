# Improving a Branch-and-Bound Approach for the  Degree-Constrained Minimum Spanning Tree Problem with LKH

This is a branch-and-bound solver for the degree-constrained minimum spanning tree problem using constraint programming and the LKH heuristic. 

Joint work of Maximilian Thiessen (University of Bonn, Fraunhofer IAIS), Luis Quesada (UCC Cork, Insight) and Kenneth N. Brown (UCC Cork, Insight)

## Dependencies:
    Java 8 (or higher)
    Choco-Solver 4.0.6 https://github.com/chocoteam/choco-solver (BSD)
    Choco-graph 4.2.4 https://github.com/chocoteam/choco-graph (BSD)
    Eclipse-Collections 10.0.0 (main jar + api) https://www.eclipse.org/collections/ (EDL,EPL)

## Data:
    res/...

    This repository contains only some example instances. To get the complete benchmark datasets see e.g. https://github.com/malbarbo/dcmstp-instances

## Build 
### Build Linux:
    javac -cp choco-graph-4.2.4-SNAPSHOT.jar:choco-solver-4.0.6-with-dependencies.jar:eclipse-collections-10.0.0.jar:eclipse-collections-api-10.0.0.jar:choco-graph-4.2.4-SNAPSHOT-tests.jar src/**/*.java

### Build Linux/Mac:
    find -name "*.java" > sources.txt
    javac -cp choco-graph-4.2.4-SNAPSHOT.jar:choco-solver-4.0.6-with-dependencies.jar:choco-graph-4.2.4-SNAPSHOT-tests.jar:eclipse-collections-10.0.0.jar:eclipse-collections-api-10.0.0.jar @sources.txt

## Test run:
    java -cp choco-graph-4.2.4-SNAPSHOT.jar:choco-graph-4.2.4-SNAPSHOT-tests.jar:choco-solver-4.0.6-with-dependencies.jar:eclipse-collections-10.0.0.jar:eclipse-collections-api-10.0.0.jar:src/ org.ucc.insight.DCMST.DCMSTTest




