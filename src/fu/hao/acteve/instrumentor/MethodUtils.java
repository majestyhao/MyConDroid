package fu.hao.acteve.instrumentor;

import fu.hao.utils.Log;
import polyglot.ast.Call;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;
import soot.util.HashChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Description:
 * Authors: Hao Fu(haofu@ucdavis.edu)
 * Date: 2016/10/5
 */
public class MethodUtils {
    private static final String TAG = MethodUtils.class.getSimpleName();

    // The definition of target methods.
    private static Set<String> TARGET_DEF = new HashSet<>();

    static {
        TARGET_DEF.add("<android.telephony.SmsManager: " +
                "void sendTextMessage(java.lang.String,java.lang.String,java.lang.String," +
                "android.app.PendingIntent,android.app.PendingIntent)>");
    }


    public static Set<SootMethod> getCalleesOf(SootMethod method) {
        Set<SootMethod> results = new HashSet<>();

        CallGraph callGraph = Scene.v().getCallGraph();
        Iterator<Edge> edgeIterator = callGraph.edgesOutOf(method);

        while (edgeIterator.hasNext()) {
            Edge edge = edgeIterator.next();
            if (edge.isExplicit()) {
                SootMethod targetMethod = edge.getTgt().method();
                results.add(targetMethod);
            }
        }

        return results;
    }

    public static Set<SootMethod> findTransitiveCalleesOf(SootMethod sootMethod) {
        /**
         * Method: findTransitiveCalleesOf
         * Description: BFS the callgraph to get all reachable callees (including grandsons) from [sootMethod]
         * @param [sootMethod]
         * @throw
         * @return java.util.Set<soot.SootMethod>
         * @author Hao Fu(haofu@ucdavis.edu)
         * @since 2016/10/5 18:03
         */
        Set<SootMethod> callees = new HashSet<>();

        CallGraph callGraph = Scene.v().getCallGraph();
        Chain<SootMethod> unprocessed = new HashChain<>();

        // Bfs the callgraph
        unprocessed.add(sootMethod);
        while (!unprocessed.isEmpty()) {
            sootMethod = unprocessed.getFirst();
            unprocessed.removeFirst();

            Iterator<Edge> edges = callGraph.edgesOutOf(sootMethod);
            while (edges.hasNext()) {
                Edge edge = edges.next();
                if (edge.isExplicit()) {
                    SootMethod tgt = edge.getTgt().method();
                    if (!callees.contains(tgt)) {
                        callees.add(tgt);
                        unprocessed.add(tgt);
                    }
                }
            }
        }

        return callees;
    }

    public static Set<SootMethod> findTransitiveCalleesOf(Collection<SootMethod> sootMethods) {
        /**
         * Method: findTransitiveCalleesOf
         * Description: Get all reachable callees (including grandsons) of sootMethods
         * @param [sootMethods]
         * @throw
         * @return java.util.Set<soot.SootMethod>
         * @author Hao Fu(haofu@ucdavis.edu)
         * @since 2016/10/5 18:19
         */
        Set<SootMethod> callees = new HashSet<>();
        for (SootMethod sootMethod : sootMethods) {
            callees.addAll(findTransitiveCalleesOf(sootMethod));
        }

        return callees;
    }

    public static boolean isOrSubClass(String className, String superClassName) {
        SootClass klass = Scene.v().getSootClass(className);
        SootClass superKlass = Scene.v().getSootClass(superClassName);

        return isOrSubClass(klass, superKlass);
    }

    public static boolean isOrSubClass(SootClass klass, SootClass superKlass) {
        if (klass.equals(superKlass)) {
            return true;
        }

        while (klass.hasSuperclass()) {
            if (klass.getSuperclass().equals(superKlass)) {
                return true;
            }
            klass = klass.getSuperclass();
        }

        return false;
    }

    public static boolean isTarget(SootMethod sootMethod) {
        /**
         * Method: isTarget
         * Description: Whether the given method is in the TARGET_DEF
         * @param [sootMethod]
         * @throw
         * @return boolean
         * @author Hao Fu(haofu@ucdavis.edu)
         * @since 2016/10/5 19:15
         */
        SootClass declaringClass = sootMethod.getDeclaringClass();
        Log.bb(TAG, declaringClass);
        for (String def : TARGET_DEF) {
            if (Scene.v().containsMethod(def)) {
                SootMethod defMethod = Scene.v().getMethod(def);
                if (isOrSubClass(declaringClass, defMethod.getDeclaringClass())) {
                    if (defMethod.getSubSignature().equals(sootMethod.getSubSignature())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static Set<SootMethod> findReachableTargets(Collection<SootMethod> startingPoints) {
        Set<SootMethod> targets = new HashSet<>();
        for (SootMethod sootMethod : findTransitiveCalleesOf(startingPoints)) {
            if (isTarget(sootMethod)) {
                targets.add(sootMethod);
            }
        }

        return targets;
    }

}
