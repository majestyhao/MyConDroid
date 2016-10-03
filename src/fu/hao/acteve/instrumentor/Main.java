package fu.hao.acteve.instrumentor;

import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.options.Options;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/**
 * Description:
 * Authors: Hao Fu(haofu@ucdavis.edu)
 * Date: 2016/10/2
 */
public class Main {

    private static String apk = "";
    private static String libJars = "./jars/a3t_symbolic.jar";
    private final static String androidJAR = "C:/Users/hao/Downloads/android-sdk-windows/platforms"; //required for CH resolution

    public static void main(String[] args) {
        soot.G.reset();
        apk = args[0];

        Options.v().set_soot_classpath("./libs/android-19.jar" + ":"
                + libJars + ":"
                //+ modelClasses + ":"
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

    }

    private static void setSootOptions() {
        //restore the class path because of soot.G.reset() in calculateSourcesSinksEntrypoints:
        Options.v().set_soot_classpath("./libs/android-19.jar" + ":" + libJars+":"
                //+modelClasses
                + ":" + apk);
        Scene.v().setSootClassPath("./libs/android-19.jar" + ":" + libJars + ":"
                //+modelClasses
                + ":" + apk);

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_src_prec(Options.src_prec_apk);

        Options.v().set_whole_program(true);	//Implicitly "on" when instrumenting Android, AFAIR.
        Options.v().setPhaseOption("cg", "on");	//"On" by default.
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
                Arrays.asList(new String[] { "acteve.symbolic.", "com.android", "models.", "org.json", "org.apache", "org.w3c",
                        "org.xml", "junit", "javax", "javax.crypto"}));

    }
}
