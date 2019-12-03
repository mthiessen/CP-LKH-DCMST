package org.ucc.insight.DCMST.helpers;

import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.solver.Solver;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.ucc.insight.DCMST.DCMSTWrapper;

import java.util.*;

public class FastTwoOptDCMST {

    private int[][] dist;
    private int n;
    private int totalCost = 0;
    private DirectedTree directedTree;
    private int outputType = 1;
    private int  kNearestNeighbours =  Integer.MAX_VALUE;
    private boolean LKH = true;
    private GraphVar g;
    private LinkedList<Integer>[] adjList;
    private Solver solver;

    /**
     * @param g must be instantiated, meaning g.LB = g.UB
     */
    public FastTwoOptDCMST(Solver solver, GraphVar g, int[][] dist, int outputType) {
        this.solver = solver;
        this.g = g;
        this.n = g.getNbMaxNodes();
        this.outputType = outputType;
        this.adjList = new LinkedList[n];

        /*
        remove this intermediate step
         */
        for (int v = 0; v < n; v++) {
            adjList[v] = new LinkedList<>();
            for (int w : g.getMandPredOrNeighOf(v)) {
                adjList[v].add(w);
            }
        }
        this.dist = dist;
        this.directedTree = new DirectedTree(adjList, dist);
        computeTotalCost(adjList);

        this.adjList = new LinkedList[n];
        for (int v = 0; v < n; v++) {
            adjList[v] = new LinkedList<>();
            for (int w  = 0; w < n; w++) {
                if (w != v)
                adjList[v].add(w);
            }

            Collections.sort(adjList[v], new DistComparator(dist[v]));
        }

    }

    private class DistComparator implements Comparator<Integer> {
        int[] dist;
        DistComparator(int[] dist) {
            this.dist = dist;
        }

        @Override
        public int compare(Integer integer, Integer t1) {
            return Integer.compare(dist[integer], dist[t1]);
        }
    }

    public FastTwoOptDCMST(LinkedList<Integer>[] adjList, int[][] dist) {
        this.dist = dist;
        this.n = adjList.length;
        this.directedTree = new DirectedTree(adjList, dist);
        computeTotalCost(adjList);
    }

    private void computeTotalCost(LinkedList<Integer>[] adjList) {
        for (int v = 0; v < n; v++) {
            for (int w : adjList[v]) {
                totalCost += dist[v][w];
            }
        }
        totalCost /= 2;
    }


    public int localEdgeSwapOptimisation() {
        return localEdgeSwapOptimisation(0, 3);
    }

    public int localEdgeSwapOptimisation(int maxDepth) {
        return localEdgeSwapOptimisation(maxDepth, 3);
    }

    public int localEdgeSwapOptimisation(int maxDepth, int kNearestNeighbours) {
        boolean foundSwap;
        do {
           foundSwap = false;
            //Edge {v,w}
            for (DirectedTreeNode v : directedTree.vertices) {
                List<DirectedTreeNode> vChildren = new LinkedList<>(v.children);
                for (DirectedTreeNode w : vChildren) {
                    Set<IntIntPair> swappedEdges = new HashSet();
                    swappedEdges.add(PrimitiveTuples.pair(Math.min(v.label,w.label), Math.max(v.label, w.label)));
                    if (!LKH) {
                        foundSwap = secondEdge(v, w, 0, maxDepth, swappedEdges);
                    } else {
                        this.kNearestNeighbours = kNearestNeighbours;
                        foundSwap = secondEdgeLK(v, w, 0, maxDepth, swappedEdges);
                    }
                    if (foundSwap) {
                        if (outputType >= 2) {
                            System.out.println(totalCost);
                            System.out.println(solver.getTimeCount() + "s");
                        }
                        break;
                    }
                }
                if (foundSwap) {
                    break;
                }
            }
        } while (foundSwap);

        return totalCost;
    }

    private boolean secondEdge(DirectedTreeNode v, DirectedTreeNode w, int neededGain, int depth, Set<IntIntPair> swappedEdges) {
        //Second Edge {x,y} not connected to v or w

        DirectedTreeNodeIterator it = (DirectedTreeNodeIterator) w.iterator();
        boolean childPhase;
        while (it.hasNext()) {
            childPhase = it.isChildPhase();
            DirectedTreeNode x = it.next();

            List<DirectedTreeNode> tempChildrenX = new LinkedList<>(x.children);

            for (DirectedTreeNode y : tempChildrenX) {

                IntIntPair xy = PrimitiveTuples.pair(Math.min(x.label,y.label), Math.max(x.label,y.label));
                if (childPhase) {
                    //gain is always negative as we are minimizing!
                    int gain = dist[v.label][x.label] + dist[y.label][w.label] - (dist[v.label][w.label] + dist[x.label][y.label]);
                    if (gain < neededGain) {
                        totalCost += gain;
                        //System.out.println(v+","+w+","+x+","+y+"| " + childPhase);
                        pathSwap(v, w, x, y);
                        swappedEdges.add(xy);
                        return true;
                    } else if (depth > 0) {
                        pathSwap(v, w, x, y);
                        swappedEdges.add(xy);
                        if (secondEdge(v, x, -gain+neededGain, depth - 1, swappedEdges)) {
                            totalCost += gain;
                            return true;
                        } else {
                            pathSwap(v, x, w, y);
                            swappedEdges.remove(xy);
                        }
                    }
                } else {
                    int gain = dist[v.label][y.label] + dist[x.label][w.label] - (dist[v.label][w.label] + dist[x.label][y.label]);
                    if (gain < neededGain) {
                        totalCost += gain;

                        //System.out.println(v+","+w+","+x+","+y+"| " + childPhase);
                        crossSwap(v,w, x, y);
                        swappedEdges.add(xy);
                        return true;
                    } else if (depth > 0) {
                        crossSwap(v,w, x, y);
                        swappedEdges.add(xy);
                        if (secondEdge(v, y, -gain+neededGain, depth - 1, swappedEdges)) {
                            totalCost += gain;
                            return true;
                        } else {
                            crossSwap(v,y, x, w);
                            swappedEdges.remove(xy);
                        }
                    }
                }
            }
        }

        return false;
    }


    /**
     * Compute the unweighted distance from b to all reachable nodes in the lower bound without using a
     * @param a
     * @param b
     * @return
     */
    private int[] distFromB(int a, int b) {
        int[] distFromB = new int[n];
        boolean[] visited = new boolean[n];

        for (int i = 0; i < n; i++) {
            distFromB[i] = Integer.MAX_VALUE;
        }

        distFromB[a] = -1;
        distFromB[b] = 0;

        visited[a] = true;
        visited[b] = true;

        LinkedList<Integer> queue = new LinkedList<>();

        queue.add(b);

        while (!queue.isEmpty()) {
            int node = queue.removeFirst();

           for (DirectedTreeNode neighbor : this.directedTree.vertices[node].children) {
                if (!visited[neighbor.label]) {
                    queue.add(neighbor.label);
                    distFromB[neighbor.label] = distFromB[node]+1;
                    visited[neighbor.label] = true;
                }

            }
        }
        return distFromB;
    }

    private boolean isParentOnRootPath(DirectedTreeNode v, DirectedTreeNode x) {
        DirectedTreeNode courser = v;

        while (courser.parent != null && courser.parent.label != courser.label) {
            if (courser.parent.label == x.label) {
                return true;
            }
            courser = courser.parent;
        }

        return false;
    }

    private boolean secondEdgeLK(DirectedTreeNode v, DirectedTreeNode w, int neededGain, int depth, Set<IntIntPair> swappedEdges) {
        //Second Edge {x,y} not connected to v or w
        int ithNearestNeighbour = 0;

        int[] distsFromW = this.distFromB(v.label,w.label);

        //distance sorted adjList
        for (int x : this.adjList[w.label]) {
            boolean childPhase;
            if (distsFromW[x] > 0 && distsFromW[x] < Integer.MAX_VALUE) {
                childPhase = true;
            } else {
                childPhase = false;
            }
            if (ithNearestNeighbour >= this.kNearestNeighbours && neededGain != 0) {
                //System.out.println("uff3");
                break;
            }

            if (!childPhase && x < v.label) {
              continue;
            }

            //andere Stelle nur bei nicht childphase oder so
            if (!childPhase && isParentOnRootPath(v, this.directedTree.vertices[x])) {
                continue;
            }

            //find unused edge! do we still need this one with the other conditions?!
            if (/*this.directedTree.vertices[w.label].children.contains(x) || */x == v.label) {
                continue;
            }



            //have to copy it, since we might change it
            List<DirectedTreeNode> tempChildrenX = new LinkedList<>(this.directedTree.vertices[x].children);

            for (DirectedTreeNode y : tempChildrenX) {

                IntIntPair xy = PrimitiveTuples.pair(Math.min(x, y.label), Math.max(x,y.label));

                if (swappedEdges.contains(xy)/* || y.label == v.label || y.label == w.label*/) {
                    continue;
                }

               if (v.children.contains(y) || y.children.contains(v) || y.children.contains(w)) {
                    System.out.println("wuupsi");
                    continue;
                }


                if (childPhase) {
                    //gain is always negative as we are minimizing!
                    int gain = dist[v.label][x] + dist[y.label][w.label] - (dist[v.label][w.label] + dist[x][y.label]);
                    if (gain < neededGain) {
                        totalCost += gain;

                        //System.out.println(v+","+w+","+x+","+y+"| " + childPhase);
                        pathSwap(v, w, this.directedTree.vertices[x], y);
                        swappedEdges.add(xy);
                        return true;
                    } else if (depth > 0) {
                        pathSwap(v, w, this.directedTree.vertices[x], y);
                        swappedEdges.add(xy);
                        if (secondEdgeLK(v, this.directedTree.vertices[x], -gain + neededGain, depth - 1, swappedEdges)) {
                            totalCost += gain;
                            return true;
                        } else {
                            pathSwap(v, this.directedTree.vertices[x], w, y);
                            swappedEdges.remove(xy);
                        }
                    }
                } else {
                    int gain = dist[v.label][y.label] + dist[x][w.label] - (dist[v.label][w.label] + dist[x][y.label]);
                    if (gain < neededGain) {
                        totalCost += gain;

                       // System.out.println(v+","+w+","+x+","+y+"| " + childPhase);
                        crossSwap(v, w, this.directedTree.vertices[x], y);
                        swappedEdges.add(xy);
                        return true;
                    } else if (depth > 0) {
                        crossSwap(v, w, this.directedTree.vertices[x], y);
                        swappedEdges.add(xy);
                        if (secondEdgeLK(v, y, -gain + neededGain, depth - 1, swappedEdges)) {
                            totalCost += gain;
                            return true;
                        } else {
                            crossSwap(v, y, this.directedTree.vertices[x], w);
                            swappedEdges.remove(xy);
                        }
                    }
                }
            }
            ithNearestNeighbour++;
            /*if (solver.isStopCriterionMet()) {
                break;
            }*/

        }
        return false;
    }



        /**
         * Swaps edges {a,b} and {c,d} with  {a,c} and {b,d}
         *  Revert also the path between b and c
         * @param a
         * @param b
         * @param d
         * @param d
         */
    private void pathSwap(DirectedTreeNode a, DirectedTreeNode b, DirectedTreeNode c, DirectedTreeNode d) {
        //the remove is probably inefficient due to linear searching...
        a.children.remove(b);
        a.children.add(c);

        DirectedTreeNode child = a;
        DirectedTreeNode node = c;
        DirectedTreeNode parent = node.parent;


        while(node != b) {

            DirectedTreeNode prevGrandParent = parent.parent;

            parent.children.remove(node);
            node.parent = child;
            parent.parent = node;
            node.children.add(parent);

            child = node;
            node = parent;
            parent = prevGrandParent;
        }

        c.children.remove(d);

        d.parent = b;
        b.children.add(d);

    }

    /**
     * Swaps edges {a,b} and {c,d} with  {a,d} and {b,c}
     * @param a
     * @param b
     * @param c
     * @param d
     */
    private void crossSwap(DirectedTreeNode a, DirectedTreeNode b, DirectedTreeNode c, DirectedTreeNode d) {
        //the remove is probably inefficient due to linear searching...
        a.children.remove(b);
        a.children.add(d);

        c.children.remove(d);
        c.children.add(b);

        d.parent = a;
        b.parent = c;
    }


}

class DirectedTreeNode implements Iterable<DirectedTreeNode> {
    int label;
    LinkedList<DirectedTreeNode> children = new LinkedList<>();
    DirectedTreeNode parent = null;
    int[][] dist;

    public DirectedTreeNode(int label, int[][] dist) {
        this.label = label;
        this.dist = dist;
    }

    @Override
    public Iterator<DirectedTreeNode> iterator() {
       return new DirectedTreeNodeIterator(this);
    }

    public Iterator<DirectedTreeNode> iterator(boolean sort) {
        return new DirectedTreeNodeIterator(this, sort);
    }

    @Override
    public String toString() {

        List<Integer> childrenLabels = new LinkedList<Integer>();
        for (DirectedTreeNode child: this.children) {
            childrenLabels.add(child.label);
        }

        return (parent != null ? parent.label + " -> |" : "|") + label + "| -> " + childrenLabels;
    }
}

/**
 * Make this an actual iterator which doesn't compute all the stuff beforehand.
 */
class DirectedTreeNodeIterator implements Iterator<DirectedTreeNode> {
        //will hold all the possible starting points for the second edge
        private LinkedList<DirectedTreeNode> queue = new LinkedList<>();

        DirectedTreeNode root;

        private Set<DirectedTreeNode> children;

        DirectedTreeNodeIterator(DirectedTreeNode root) {
            this(root, false);
        }

    /**
     * Root is the lower end of the first edge!
     * @param root
     */
    DirectedTreeNodeIterator(DirectedTreeNode root, boolean sort) {
            this.root = root;

            //add all children
            doBFS(root, null);

            children = new HashSet(queue);




            //add all left siblings
            DirectedTreeNode parent = root.parent;
           if (parent != null) {
               for (DirectedTreeNode sibling : parent.children) {
                   if (sibling == root) {
                       break;
                   }
                   for (DirectedTreeNode child : sibling.children)
                       doBFS(child, null);
                   }
           }


            //
            DirectedTreeNode prevParent = root.parent;
            if (parent != null) {
                parent = prevParent.parent;
                while (parent != null) {
                    doBFS(parent, prevParent);
                    prevParent = parent;
                    parent = parent.parent;
                }
            }

            if (sort) {
                this.queue.sort(new Comparator<DirectedTreeNode>() {
                    @Override
                    public int compare(DirectedTreeNode t1, DirectedTreeNode t2) {
                        return Integer.compare(root.dist[root.label][t1.label], root.dist[root.label][t2.label]);
                    }
                });
            }
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public DirectedTreeNode next() {
            return queue.removeFirst();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean isChildPhase() {
            return this.children.contains(queue.get(0));
        }


    /**
     * Do a BFS starting from v but only upto (not included) the child notAllowed
     * @param v
     * @param notAllowed
     */
    private void doBFS(DirectedTreeNode v, DirectedTreeNode notAllowed) {

        LinkedList<DirectedTreeNode> bfsQueue = new LinkedList<>();
        bfsQueue.add(v);

        while (!bfsQueue.isEmpty()) {
            DirectedTreeNode node = bfsQueue.removeFirst();

            for (DirectedTreeNode neighbor : node.children) {
               if (neighbor != notAllowed) {
                   if (neighbor.children.size() > 0) {
                       queue.add(neighbor);
                       bfsQueue.add(neighbor);
                   }
               } else {
                   break;
               }

            }
        }
    }


}

class DirectedTree {
    DirectedTreeNode[] vertices;
    int root;
    int n;
    LinkedList<Integer>[] adjList;
    int[][] dist;

    DirectedTree(LinkedList<Integer>[] adjList, int root, int[][] dist) {
        this.adjList = adjList;
        this.n = adjList.length;
        this.vertices = new DirectedTreeNode[this.n];
        this.root = root;
        this.dist = dist;
        constructDirectedTree();
    }

    DirectedTree(LinkedList<Integer>[] adjList, int[][] dist) {
        this(adjList, 0, dist);
    }

    /**
     * BFS traversal
     */
    private void constructDirectedTree() {
        boolean[] marked = new boolean[n];

        LinkedList<Integer> queue = new LinkedList<>();
        queue.add(root);
        vertices[root] = new DirectedTreeNode(root, dist);
        marked[root] = true;

        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            for (int neighbor : this.adjList[node]) {
                if (marked[neighbor] == false) {
                    queue.add(neighbor);
                    marked[neighbor] = true;

                    //construct new node
                    vertices[neighbor] = new DirectedTreeNode(neighbor, dist);
                    vertices[neighbor].parent = vertices[node];
                    vertices[node].children.add(vertices[neighbor]);
                }

            }
        }
    }




}