package org.ucc.insight.DCMST.helpers;

import java.util.Arrays;
import java.util.Comparator;

public class MST {

    /*
    Kruskal's MST
     */
    public static int mst(int[] adj, int n) {
        int mstWeight;
        int[] component = new int[n];
        int[][] edges = new int[n * n][2];
        for (int i = 0; i < n; i++) {
            component[i] = i;
            for (int j = 0; j < n; j++) {
                edges[i * n + j] = new int[]{i, j};
            }
        }

        Arrays.sort(edges, new Comparator<int[]>() {
            @Override
            public int compare(int[] ints, int[] t1) {
                int leftDist = ints[0] != ints[1] ? adj[ints[0] * n + ints[1]] : Integer.MAX_VALUE;
                int rightDist = t1[0] != t1[1] ? adj[t1[0] * n + t1[1]] : Integer.MAX_VALUE;


                return Integer.compare(leftDist, rightDist);
            }
        });


        int[] currentEdge = edges[0]; //start with lightest
        component[currentEdge[0]] = component[currentEdge[1]];
        int edgeCount = 1;
        mstWeight = adj[currentEdge[0] * n + currentEdge[1]];

        int currentEdgeIndex = 1;
        while (edgeCount < n - 1) {
            do {
                currentEdge = edges[currentEdgeIndex];
                currentEdgeIndex++;
            } while (component[currentEdge[0]] == component[currentEdge[1]]); //until edge introduces no loop!

            mstWeight += adj[currentEdge[0] * n + currentEdge[1]];
            int oldComponent = component[currentEdge[1]];
            int newComponent = component[currentEdge[0]];
            for (int i = 0; i < n; i++) {
                if (component[i] == oldComponent) {
                    component[i] = newComponent;
                }
            }
            edgeCount++;
        }

        return mstWeight;
    }

}
