package fu.hao.acteve.instrumentor;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import fu.hao.utils.Log;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.*;
import soot.options.Options;
import soot.util.Chain;

/**
 * Description: A instrumentation example.
 * Authors: Hao Fu(haofu@ucdavis.edu)
 * Date: 2016/10/4
 */
public class Test {
    private static final String TAG = Test.class.getSimpleName();

    public static void main(String[] args) {
        //initialise the options set
        Options.v().set_android_jars("C:/Users/hao/Downloads/android-sdk-windows/platforms/");
        //Options.v().set_force_android_jar("D:/workspace/ConDroid/libs/android-19.jar");
        Options.v().set_soot_classpath("C:/Users/hao/Downloads/android-sdk-windows/platforms/android-19/android.jar");
        Options.v().set_process_dir(Collections.singletonList("D:/workspace/Button1/app/app-release.apk"));
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_dir("sootOutput/");
        //prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk);
        //Options.v().set_src_prec(Options.src_prec_jimple);
        //output as APK, too//-f J
        //Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_output_format(Options.output_format_dex);


        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

            @Override
            protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
                final PatchingChain<Unit> units = b.getUnits();
                final SootMethod toCall = Scene.v().getSootClass("android.util.Log").getMethod("int i(java.lang.String,java.lang.String)");

                //important to use snapshotIterator here
                for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext(); ) {
                    final Unit u = iter.next();
                    u.apply(new AbstractStmtSwitch() {

                        public void caseInvokeStmt(InvokeStmt stmt) {
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            if (invokeExpr.getMethod().getName().equals("loadUrl")) {
                                Local tmpRef = addTmpRef(b);
                                //Local tmpString = addTmpString(b);

                                // insert "tmpRef = java.lang.System.out;"
                              /*  units.insertBefore(Jimple.v().newAssignStmt(
						                      tmpRef, Jimple.v().newStaticFieldRef(
						                      Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);
						       	*/
                                // insert "tmpLong = 'HELLO';"
                                units.insertBefore(Jimple.v().newAssignStmt(tmpRef,
                                        StringConstant.v("HELLO")), u);

                                // insert "tmpRef.println(tmpString);"
                                //SootMethod toCall = Scene.v().getSootClass("android.util.Log").getMethod("int v(java.lang.String, java.lang.String)");
                                units.insertBefore(Jimple.v().newInvokeStmt(
                                        Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), tmpRef)), u);

                                //check that we did not mess up the Jimple
                                b.validate();
                            }
                        }
                    });
                }
            }

        }));


        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter2", new BodyTransformer() {
            @Override
            protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
                final PatchingChain<Unit> units = b.getUnits();
                SootMethod log = Scene.v().getSootClass("android.util.Log").getMethod("int i(java.lang.String,java.lang.String)");

                //important to use snapshotIterator here

                for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext(); ) {

                    Stmt s = (Stmt) iter.next();

                    if (s instanceof InvokeStmt || s instanceof ReturnStmt) {
                        //make new static invokement
                        InvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr(log.makeRef(), (StringConstant.v("qwerty12345")), StringConstant.v("-If you are reading this, it has worked"));
                        // turn it into an invoke statement
                        Stmt incStmt = Jimple.v().newInvokeStmt(invokeExpr);
                        //insert into chain
                        units.insertBefore(incStmt, s);
                        //.newAssignStmt(tmpRef, Jimple.v().newStaticFieldRef(
                        //Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), s);
                        //
                    }

                }
            }
            //end of internalTransform declaration
        }));

        //soot.Main.main(args); // for official jar

        Scene.v().loadNecessaryClasses();
        SootMethod method = Scene.v().getMethod("<android.util.Log: int d(java.lang.String,java.lang.String)>");
        Log.msg(TAG, method);

        // 执行Soot, 在这里即完成嵌入操作
        PackManager.v().runPacks();
        PackManager.v().writeOutput();
    }

    private static Local addTmpRef(Body body) {
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("android.util.Log"));
        body.getLocals().add(tmpRef);
        return tmpRef;
    }
}

