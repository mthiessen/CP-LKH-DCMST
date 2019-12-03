package org.ucc.insight.DCMST;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.search.strategy.GraphSearch;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.objective.ObjectiveStrategy;
import org.chocosolver.solver.objective.OptimizationPolicy;
import org.chocosolver.solver.search.loop.monitors.IMonitorDownBranch;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.loop.monitors.IMonitorUpBranch;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.tuple.primitive.IntIntPairImpl;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.ucc.insight.DCMST.helpers.FastTwoOptDCMST;
import org.ucc.insight.DCMST.helpers.MST;

import java.io.*;
import java.util.*;

public class DCMSTWrapper {

    static final int NONE = 0;
    static final int FINAL_STATS = 1;
    static final int FOUND_SOLUTIONS = 2;
    static final int OBJECTIVE_CHANGES = 3;
    static final int SEARCH_TREE = 4;

    private int outputType = 1;


    // input
    private int n;
    private int[] dMax;
    private int[][] dist;
    private int lb, ub;
    private String instance;
    private boolean firstTime = true;


    private String limitTime = "10800s";



    public DCMSTWrapper(int n, int[] dMax, int[][] dist, int lb, int ub, String instance) {
        this.n = n;
        this.dMax = dMax;
        this.dist = dist;
        this.lb = lb;
        this.ub = ub;
        this.instance = instance;
    }

    /**
     * Bottom-up with last-conflict and Held-Karp (Fages, J. G., Lorca, X., & Rousseau, L. M. The Salesman and the Tree: the importance of search in CP.)
     */
    public int solveBottomUp() {
        GraphModel model = new GraphModel();
        IntVar totalCost = model.intVar("obj", lb, ub, true);
        // graph var domain
        UndirectedGraph GLB = new UndirectedGraph(model,n, SetType.LINKED_LIST,true);
        UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.BIPARTITESET,true);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (dist[i][j] != -1 && !(dMax[i] == 1 && dMax[j] == 1)) {
                    GUB.addEdge(i, j); // possible edge
                }
            }
        }
        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);
        IntVar[] degrees = model.degrees(graph);
        for (int i = 0; i < n; i++) {
            model.arithm(degrees[i], "<=", dMax[i]).post();
        }

        // degree constrained-minimum spanning tree constraint
        model.dcmst(graph,degrees,totalCost,dist,2).post();

        final GraphSearch mainSearch = new GraphSearch(graph, dist);
        // find the first solution by selecting cheap edges
        mainSearch.configure(GraphSearch.MIN_COST);
        Solver s = model.getSolver();
        // then select the most expensive ones (fail first principle, with last conflict)

        s.plugMonitor((IMonitorSolution) () -> {
            mainSearch.useLastConflict();
            mainSearch.configure(GraphSearch.MIN_P_DEGREE);
            if (outputType >= FOUND_SOLUTIONS) {
                System.out.println("Solution found : "+totalCost);
            }

            if (outputType >= OBJECTIVE_CHANGES) {
                totalCost.addMonitor(new IVariableMonitor() {
                    @Override
                    public void onUpdate(Variable var, IEventType evt) throws ContradictionException {
                        if (evt == IntEventType.INSTANTIATE)
                            System.out.println(var +  " " + evt + " || " + "#backtracks: " + s.getBackTrackCount()+ " || " + "#fails: " + s.getFailCount());
                    }
                });
            }

            if (outputType >= SEARCH_TREE) {


                s.plugMonitor(new IMonitorDownBranch() {
                    @Override
                    public void beforeDownBranch(boolean left) {
                        System.out.println((left ? "<-/ " : "\\-> ")+ s.getDecisionPath()  +  " || " + totalCost);

                    }
                });

                s.plugMonitor(new IMonitorUpBranch() {
                    @Override
                    public void afterUpBranch() {
                        System.out.println("↑ " +  s.getDecisionPath() +  " || " + totalCost);

                    }
                });
            }
        });

        // bottom-up optimization : find a first solution then reach the global minimum from below
        s.setSearch(new ObjectiveStrategy(totalCost, OptimizationPolicy.BOTTOM_UP), mainSearch);
        s.limitTime(limitTime);
        s.limitSolution(2);
        // find optimum
        model.setObjective(Model.MINIMIZE,totalCost);
        while (s.solve()){
            if (outputType >= FOUND_SOLUTIONS)  {
                System.out.println(totalCost);
            }
        }

        if (outputType >= FINAL_STATS) {
            s.printStatistics();
        }

        return (int) s.getBestSolutionValue();
    }


   
    public void solveBranchAndBound() {
        solveBranchAndBound(false,0,0);
    }

    public void solveBranchAndBound(boolean useLKH) {
        solveBranchAndBound(useLKH, 0, 3);

    }

    public void solveBranchAndBound(boolean useLKH, int twoOptDepth) {
        solveBranchAndBound(useLKH, twoOptDepth, 3);

    }

    public void solveBranchAndBound(boolean useLKH, int twoOptDepth, int nearestNeighbor) {
        GraphModel model = new GraphModel();
        int mst = MST.mst(ArrayUtils.flatten(this.dist), n);
        //System.out.println(mst +  "   ||||  " + (int) (1.5*mst));
        IntVar totalCost = model.intVar("obj",mst,ub, true);
        // graph var domain
        UndirectedGraph GLB = new UndirectedGraph(model,n,SetType.LINKED_LIST,true);
        UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.BIPARTITESET,true);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (dist[i][j] != -1 && !(dMax[i] == 1 && dMax[j] == 1)) {
                    GUB.addEdge(i, j); // possible edge
                }
            }
        }
        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);
        IntVar[]degrees = model.degrees(graph);
        for (int i = 0; i < n; i++) {
            model.arithm(degrees[i], "<=", dMax[i]).post();
        }

        // degree constrained-minimum spanning tree constraint
        model.dcmst(graph,degrees,totalCost,dist,2).post();


        final GraphSearch mainSearch = new GraphSearch(graph, dist);
        // find the first solution by selecting cheap edges
        mainSearch.configure(GraphSearch.MIN_COST);
        Solver s = model.getSolver();
        s.setSearch(mainSearch);


        firstTime = true;

        Map<IntIntPair, BoolVar> isEdge = new HashMap<>();
        s.plugMonitor((IMonitorSolution) () -> {

            if (outputType >= FOUND_SOLUTIONS) {
                System.out.println("Solution found : "+totalCost);
            }
            if (firstTime) {
                mainSearch.useLastConflict();
                mainSearch.configure(GraphSearch.MIN_P_DEGREE);


                if (outputType >= OBJECTIVE_CHANGES) {
                    totalCost.addMonitor(new IVariableMonitor() {
                        @Override
                        public void onUpdate(Variable var, IEventType evt) throws ContradictionException {
                            if (evt == IntEventType.INSTANTIATE)
                                System.out.println(var +  " " + evt + " || " + "#backtracks: " + s.getBackTrackCount()+ " || " + "#fails: " + s.getFailCount());
                        }
                    });
                }

                if (outputType >= SEARCH_TREE) {


                    s.plugMonitor(new IMonitorDownBranch() {
                        @Override
                        public void beforeDownBranch(boolean left) {
                            System.out.println((left ? "<-/ " : "\\-> ")+ s.getDecisionPath().getLastDecision()  +  " || " + totalCost);

                        }
                    });

                    s.plugMonitor(new IMonitorUpBranch() {
                        @Override
                        public void afterUpBranch() {
                            System.out.println("↑ " +  s.getDecisionPath().getLastDecision() +  " || " + totalCost);

                        }
                    });
                }

                setFirstTime(false);

            }
            if (useLKH) {
                System.out.println(s.getTimeCount()+"s");
                FastTwoOptDCMST upperBoundHeuristic = new FastTwoOptDCMST(s, graph, this.dist, outputType);
                int newUpperBound = upperBoundHeuristic.localEdgeSwapOptimisation(twoOptDepth, nearestNeighbor);
                model.arithm(totalCost, "<=", newUpperBound-1).post();
                System.out.println(s.getTimeCount()+"s");

            }

        });

          s.limitTime(limitTime);

        // find optimum
        model.setObjective(Model.MINIMIZE,totalCost);
        while (s.solve()){
            if (outputType >= FOUND_SOLUTIONS)  {
                System.out.println(totalCost);
            }
            System.out.println(s.getTimeCount()+"s");
            if (s.getSolutionCount() == 1) {
                /**
                 * This reset exactly here is crucial to guarantee complete search with HeldKarpWCCPropagator with lagrMode: 2
                 */
                s.restart();
            }
        }

        if (outputType >= FINAL_STATS) {
            s.printStatistics();
        }
    }


    

    public boolean isFirstTime() {
        return firstTime;
    }

    public void setFirstTime(boolean firstTime) {
        this.firstTime = firstTime;
    }


    public int getOutputType() {
        return outputType;
    }

    public void setOutputType(int outputType) {
        this.outputType = outputType;
    }


    public String getLimitTime() {
        return limitTime;
    }

    public void setLimitTime(String limitTime) {
        this.limitTime = limitTime;
    }

}
