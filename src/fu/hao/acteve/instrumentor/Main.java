package fu.hao.acteve.instrumentor;

import fu.hao.utils.Log;
import fu.hao.utils.Settings;
import org.xml.sax.SAXException;
import soot.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.options.Options;
import soot.util.Chain;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static soot.SootClass.SIGNATURES;

/**
 * Description:
 * Authors: Hao Fu(haofu@ucdavis.edu)
 * Date: 2016/10/2
 */
public class Main extends SceneTransformer {
    private static final String TAG = Main.class.getSimpleName();

    private static String apk = "";
    private static String libJars = "jars/a3t_symbolic.jar";
    private final static String androidJAR = "C:/Users/hao/Downloads/android-sdk-windows/platforms"; //required for CH resolution
    private final static String modelClasses = "D:/workspace/ConDroid/mymodels/src";
    private static InstrumentationHelper instrumentationHelper;
    private static Set<SootMethod> methodsToInstrument = new HashSet<>();

    private static boolean SKIP_CONCOLIC_INSTRUMENTATION = false;
    private static boolean SKIP_ALL_INSTRUMENTATION = false;        // Switch off all instrumentation for debugging
    private static boolean SKIP_CG_EXTENTION = false;                    // Extends the CG by direct calls to callbacks

    /**
     * Method: main
     * Description:
     * 1. Find all reachable target methods
     * 2. Find all lifecycle entry points
     * 3. Determine all paths from methods in 2. to 1.
     * 4. Instrument those paths
     * Authors：Hao Fu(haofu@ucdavis.edu)
     * Date: 2016/10/5 16:41
     */
    public static void main(String[] args) throws XPathExpressionException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        Settings.setLogLevel(0);

        soot.G.reset();
        apk = args[0];

        Options.v().set_soot_classpath("tools/android-19.jar;"
                + libJars + ";"
                //+ modelClasses + ";"
                + apk);

        // inject correct dummy main:
        SetupApplication setupApplication = new SetupApplication(androidJAR, apk);

        try {
            /** ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !
             *  ! NOTE: calculateSourcesSinksEntrypoints() calls soot.G.reset()
             *  , i.e. it clears all global settings! ! ! ! ! ! ! ! ! ! ! ! ! !
             */
            setupApplication.calculateSourcesSinksEntrypoints(new HashSet<AndroidMethod>(), new HashSet<AndroidMethod>());
            //setupApplication.calculateSourcesSinksEntrypoints("./SourcesAndSinks.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }

        setSootOptions();
        Scene.v().loadNecessaryClasses(); // The original version does not need this statement, do not know why

        //Create dummy main method referencing all entry points
        SootMethod dummyMain = setupApplication.getEntryPointCreator().createDummyMain();

        Scene.v().setEntryPoints(Collections.singletonList(dummyMain));
        Scene.v().addBasicClass(dummyMain.getDeclaringClass().getName(), SootClass.BODIES);
        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("cg").apply();
        // Step 1: Find all lifecycle entry points
        Set<SootMethod> entryPoints = MethodUtils.getCalleesOf(dummyMain);

        if (Settings.isDebug()) {
            for (SootMethod entry : entryPoints) {
                Log.debug(TAG, "Found entry: " + entry);
            }
        }

        // Step 2: Find all target methods
        Set<SootMethod> targetMethods = MethodUtils.findReachableTargets(entryPoints);
        Log.msg(TAG, "Found the following target methods:");
        for (SootMethod m : targetMethods){
            Log.msg(TAG, m.getSignature());
        }

        /*
        //Register all application classes for instrumentation
        Chain<SootClass> appclasses = Scene.v().getApplicationClasses();
        for (SootClass c : appclasses) {
            methodsToInstrument.addAll(c.getMethods());
        }

        //Collect additional classes which will be injected into the app
        List<String> libClassesToInject = SourceLocator.v().getClassesUnder("jars/a3t_symbolic.jar");
        for (String s : libClassesToInject) {
            Scene.v().addBasicClass(s, SootClass.BODIES);
            Scene.v().loadClassAndSupport(s);
            SootClass clazz = Scene.v().forceResolve(s, SootClass.BODIES);
            clazz.setApplicationClass();
        }

        //Get the lifecycle method to instrument
        instrumentationHelper = new InstrumentationHelper(new File(apk));
        SootMethod lcMethodToExtend = instrumentationHelper.getDefaultOnResume();
        if (lcMethodToExtend == null) {
            lcMethodToExtend = instrumentationHelper.getDefaultOnCreate();
        }

        assert lcMethodToExtend != null : "No default activity found";

        if (!SKIP_CG_EXTENTION) {
            //PackManager.v().getPack("wjtp").add(new Transform("wjtp.android", new AndroidCGExtender()));
        }

        if (!SKIP_CONCOLIC_INSTRUMENTATION && !SKIP_ALL_INSTRUMENTATION) {
            PackManager.v().getPack("wjtp").add(new Transform("wjtp.acteve", new Main()));
        }*/
    }

    private static void setSootOptions() {
        //restore the class path because of soot.G.reset() in calculateSourcesSinksEntrypoints:
        Options.v().set_soot_classpath("tools/android-19.jar;" + libJars + ";"
                //+ modelClasses + ";"
                + apk);
        Scene.v().setSootClassPath("tools/android-19.jar;" + libJars + ";"
                //+ modelClasses + ";"
                + apk);

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_src_prec(Options.src_prec_apk);

        Options.v().set_whole_program(true);    //Implicitly "on" when instrumenting Android, AFAIR.
        Options.v().setPhaseOption("cg", "on");    //"On" by default.
        Options.v().setPhaseOption("cg", "verbose:true");
        Options.v().setPhaseOption("cg", "safe-newinstance:true");
        Options.v().setPhaseOption("cg", "safe-forname:true");
        Options.v().set_keep_line_number(true);
        Options.v().set_keep_offset(true);

        // replace Soot's printer with our logger (will be overwritten by G.reset(), though)
        // G.v().out = new PrintStream(new LogStream(Logger.getLogger("SOOT"),
        // Level.DEBUG), true);

        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        boolean VALIDATE = true;
        Options.v().set_validate(VALIDATE);

        boolean DUMP_JIMPLE = true;

        if (DUMP_JIMPLE) {
            Options.v().set_output_format(Options.output_format_jimple);
        } else {
            Options.v().set_output_format(Options.output_format_dex);
        }
        Options.v().set_process_dir(Collections.singletonList(apk));
        Options.v().set_force_android_jar(androidJAR);
        Options.v().set_android_jars(libJars);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_debug(true);

        // All packages which are not already in the app's transitive hull but
        // are required by the injected code need to be marked as dynamic.
        Options.v().set_dynamic_package(
                Arrays.asList(new String[]{"acteve.symbolic.", "com.android", "models.", "org.json", "org.apache", "org.w3c",
                        "org.xml", "junit", "javax", "javax.crypto"}));


        Scene.v().addBasicClass("android.util.Log", SIGNATURES);
    }


    /**
     * Method: internalTransform
     * Description:
     * Authors：Hao Fu(haofu@ucdavis.edu)
     * Date: 2016/10/2 22:58
     */
    @Override
    protected void internalTransform(String s, Map<String, String> map) {

    }
}
