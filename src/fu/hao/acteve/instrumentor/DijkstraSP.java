package fu.hao.acteve.instrumentor;

/**
 * Description:
 * Authors: Hao Fu(haofu@ucdavis.edu)
 * Date: 2016/10/5
 */

import fu.hao.utils.IndexMinPQ;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

/**
 *  The {@code DijkstraSP} class represents a data type for solving the
 *  single-source shortest paths problem in edge-weighted digraphs
 *  where the edge weights are nonnegative.
 *  <p>
 *  This implementation uses Dijkstra's algorithm with a binary heap.
 *  The constructor takes time proportional to <em>E</em> log <em>V</em>,
 *  where <em>V</em> is the number of vertices and <em>E</em> is the number of edges.
 *  Afterwards, the {@code distTo()} and {@code hasPathTo()} methods take
 *  constant time and the {@code pathTo()} method takes time proportional to the
 *  number of edges in the shortest path returned.
 *  <p>
 *  For additional documentation,
 *  see <a href="http://algs4.cs.princeton.edu/44sp">Section 4.4</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class DijkstraSP {
    Map<SootMethod, Integer> vertices;
    private Map<SootMethod, Integer> distTo;
    //private double[] distTo;          // distTo[v] = distance of shortest s->v path
    private Map<SootMethod, Edge> edgeTo;
    //private Edge[] edgeTo;    // edgeTo[v] = last edge on shortest s->v path
    private IndexMinPQ<Integer> pq;    // priority queue of vertices

    /**
     * Computes a shortest-paths tree from the source vertex {@code s} to every other
     * vertex in the edge-weighted digraph {@code G}.
     *
     * @param  callGraph the edge-weighted digraph
     * @param  source the source vertex
     * @throws IllegalArgumentException if an edge weight is negative
     * @throws IllegalArgumentException unless {@code 0 <= s < V}
     */
    public DijkstraSP(CallGraph callGraph, SootMethod source) {
        vertices = new HashMap<>();
        Iterator<MethodOrMethodContext> methodIterator = callGraph.sourceMethods();
        int index = 0;
        for (Edge edge : callGraph) {
            if (!vertices.containsKey(edge.src())) {
                vertices.put(edge.src(), index++);
            }
            if (!vertices.containsKey(edge.tgt())) {
                vertices.put(edge.tgt().method(), index++);
            }
        }

        distTo = new HashMap<>();
        edgeTo = new HashMap<>();

        for (SootMethod vertex : vertices.keySet()) {
            distTo.put(vertex, Integer.MAX_VALUE);
        }
        distTo.replace(source, 0);

        // relax vertices in order of distance from s
        pq = new IndexMinPQ<>(vertices.size());
        pq.insert(vertices.get(source), distTo.get(source));
        while (!pq.isEmpty()) {
            int v = pq.delMin();
            SootMethod vertex = null;
            for (Map.Entry<SootMethod, Integer> entry : vertices.entrySet()) {
                if (v == entry.getValue()) {
                    vertex = entry.getKey();
                }
            }
            Iterator<Edge> edges = callGraph.edgesOutOf(vertex);
            while (edges.hasNext()) {
                Edge edge = edges.next();
                if (edge.isExplicit()) {
                    relax(edge);
                }
            }
        }

        // check optimality conditions
        assert check(callGraph, source);
    }

    // relax edge e and update pq if changed
    private void relax(Edge edge) {
        SootMethod v = edge.src();
        SootMethod w = edge.tgt();
        if (distTo.get(w) > distTo.get(v) + 1) {
            distTo.put(w, distTo.get(v) + 1);
            edgeTo.put(w, edge);
            if (pq.contains(vertices.get(w))) {
                pq.decreaseKey(vertices.get(w), distTo.get(w));
            } else {
                pq.insert(vertices.get(w), distTo.get(w));
            }
        }
    }

    public int distTo(SootMethod t) {
        return distTo.get(t);
    }

    /**
     * Returns true if there is a path from the source vertex {@code s} to vertex {@code v}.
     *
     * @param  v the destination vertex
     * @return {@code true} if there is a path from the source vertex
     *         {@code s} to vertex {@code v}; {@code false} otherwise
     */
    public boolean hasPathTo(SootMethod v) {
        return distTo.get(v) < Integer.MAX_VALUE;
    }

    /**
     * Returns a shortest path from the source vertex {@code s} to vertex {@code v}.
     *
     * @param  v the destination vertex
     * @return a shortest path from the source vertex {@code s} to vertex {@code v}
     *         as an iterable of edges, and {@code null} if no such path
     */
    public List<Edge> pathTo(SootMethod v) {
        if (!hasPathTo(v)) {
            return null;
        }
        //Stack<Edge> path = new Stack<>();
        LinkedList<Edge> path = new LinkedList<>();
        for (Edge e = edgeTo.get(v); e != null; e = edgeTo.get(e.src())) {
            //path.push(e);
            path.addFirst(e);
        }
        return path;
    }


    // check optimality conditions:
    // (i) for all edges e:            distTo[e.to()] <= distTo[e.from()] + e.weight()
    // (ii) for all edge e on the SPT: distTo[e.to()] == distTo[e.from()] + e.weight()
    private boolean check(CallGraph callGraph, SootMethod s) {
        // check that distTo[v] and edgeTo[v] are consistent
        if (distTo.get(s) != 0.0 || edgeTo.get(s) != null) {
            System.err.println("distTo[s] and edgeTo[s] inconsistent");
            return false;
        }
        for (SootMethod v : vertices.keySet()) {
            if (v == s) {
                continue;
            }
            if (edgeTo.get(v) == null && distTo.get(v) != Double.POSITIVE_INFINITY) {
                System.err.println("distTo[] and edgeTo[] inconsistent");
                return false;
            }
        }

        // check that all edges e = v->w satisfy distTo[w] <= distTo[v] + e.weight()
        for (SootMethod v : vertices.keySet()) {
            Iterator<Edge> edges = callGraph.edgesOutOf(v);

            while (edges.hasNext()) {
                Edge e = edges.next();
                SootMethod w = e.tgt();
                if (distTo.get(v) + 1< distTo.get(w)) {
                    System.err.println("edge " + e + " not relaxed");
                    return false;
                }
            }
        }

        // check that all edges e = v->w on SPT satisfy distTo[w] == distTo[v] + e.weight()
        for (SootMethod w : vertices.keySet()) {
            if (edgeTo.get(w) == null) {
                continue;
            }

            Edge e = edgeTo.get(w);
            SootMethod v = e.src();
            if (!w.equals(e.tgt())) {
                return false;
            }

            if (distTo.get(v) + 1 != distTo.get(w)) {
                System.err.println("edge " + e + " on shortest path not tight");
                return false;
            }
        }
        return true;
    }

    public Map<SootMethod, Integer> getVertices() {
        return vertices;
    }


}

