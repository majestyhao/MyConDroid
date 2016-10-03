package fu.hao.acteve.instrumentor;

import fu.hao.utils.Log;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: Helper class for instrumenting bytecode artifacts.
 * Authors: Hao Fu(haofu@ucdavis.edu)
 * Date: 2016/10/2
 */
public class InstrumentationHelper {
    private String TAG = this.getClass().getSimpleName();

    private String manifest, packagename;

    private Set<String> mainActivities;

    /**
     * All listener classes defined inside Android.view. We need those to
     * determine which fields of activity classes are listeners to invoke them
     * during instrumentation.
     */
    private static HashMap<String, String[]> uiListeners = new HashMap<String, String[]>();
    static {
        HashMap<String, String[]> aMap = new HashMap<String, String[]>();
        aMap.put("android.view.ActionProvider$VisibilityListener", new String[] {"void onActionProviderVisibilityChanged(boolean)"});
        aMap.put("android.view.GestureDetector$OnDoubleTapListener", new String[] {"boolean onDoubleTap(android.view.MotionEvent)", "boolean onDoubleTapEvent(android.view.MotionEvent)", "boolean onSingleTapConfirmed(android.view.MotionEvent)" });
        aMap.put("android.view.GestureDetector$OnGestureListener", new String[]{"boolean onDown(MotionEvent)", "boolean onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)", "void onLongPress(android.view.MotionEvent)", "boolean onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)", "void onShowPress(android.view.MotionEvent)", "boolean onSingleTapUp(android.view.MotionEvent)"});
        aMap.put("android.view.MenuItem$OnActionExpandListener", new String[]{"boolean onMenuItemActionCollapse(android.view.MenuItem)", "boolean onMenuItemActionExpand(android.view.MenuItem)"}) ;
        aMap.put("android.view.MenuItem$OnMenuItemClickListener", new String[]{"boolean onMenuItemClick(android.view.MenuItem)"});
        aMap.put("android.view.ScaleGestureDetector$OnScaleGestureListener", new String[]{"boolean onScale(android.view.ScaleGestureDetector)", "boolean onScaleBegin(android.view.ScaleGestureDetector)", "void onScaleEnd(android.view.ScaleGestureDetector)"});
        aMap.put("android.view.TextureView$SurfaceTextureListener", new String[]{"void onSurfaceTextureAvailable(android.graphics.SurfaceTexture, int, int)", "boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture)", "void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture, int, int)", "void onSurfaceTextureUpdated(android.graphics.SurfaceTexture)"});
        aMap.put("android.view.View$OnAttachStateChangeListener", new String[]{"void onViewAttachedToWindow(android.view.View)", "void onViewDetachedFromWindow(android.view.View)"});
        aMap.put("android.view.View$OnClickListener", new String[]{"void onClick(android.view.View)"});
        aMap.put("android.view.View$OnCreateContextMenuListener", new String[]{"void onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)"});
        aMap.put("android.view.View$OnDragListener", new String[]{"boolean onDrag(android.view.View, android.view.DragEvent)"});
        aMap.put("android.view.View$OnFocusChangeListener", new String[]{"void onFocusChange(android.view.View, boolean)"});
        aMap.put("android.view.View$OnGenericMotionListener", new String[]{"boolean onGenericMotion(android.view.View, MotionEvent)"});
        aMap.put("android.view.View$OnHoverListener", new String[]{"boolean onHover(android.view.View, MotionEvent)"});
        aMap.put("android.view.View$OnKeyListener", new String[]{"boolean onKey(android.view.View, int, KeyEvent)"});
        aMap.put("android.view.View$OnLayoutChangeListener", new String[]{"void onLayoutChange(android.view.View, int, int, int, int, int, int, int, int)"});
        aMap.put("android.view.View$OnLongClickListener", new String[]{"boolean onLongClick(android.view.View)"});
        aMap.put("android.view.View$OnSystemUiVisibilityChangeListener", new String[]{"void onSystemUiVisibilityChange(int)"});
        aMap.put("android.view.View$OnTouchListener", new String[]{"boolean onTouch(android.view.View, android.view.MotionEvent)"});
        aMap.put("android.view.ViewGroup$OnHierarchyChangeListener", new String[]{"void onChildViewAdded(android.view.View, android.view.View)", "void onChildViewRemoved(android.view.View, android.view.View)"});
        aMap.put("android.view.ViewStub$OnInflateListener", new String[]{"void onInflate(android.view.ViewStub, android.view.View)"});
        aMap.put("android.view.ViewTreeObserver$OnDrawListener", new String[]{"void onDraw()"});
        aMap.put("android.view.ViewTreeObserver$OnGlobalFocusChangeListener", new String[]{"void onGlobalFocusChanged(android.view.View, android.view.View)"});
        aMap.put("android.view.ViewTreeObserver$OnGlobalLayoutListener", new String[]{"void onGlobalLayout()"});
        aMap.put("android.view.ViewTreeObserver$OnPreDrawListener", new String[]{"boolean onPreDraw()"}); //TODO: do we really need ViewTreeObserver?
        aMap.put("android.view.ViewTreeObserver$OnScrollChangedListener", new String[]{"void onScrollChanged()"});
        aMap.put("android.view.ViewTreeObserver$OnTouchModeChangeListener", new String[]{"void onTouchModeChanged(boolean)"});
        aMap.put("android.view.ViewTreeObserver$OnWindowAttachListener", new String[]{"void onWindowAttached()", "void onWindowDetached()"});
        aMap.put("android.view.ViewTreeObserver$OnWindowFocusChangeListener", new String[]{"void onWindowFocusChanged(boolean)"});
        uiListeners = aMap;
    };

    /**
     *
     * @param apkFile
     *            APK File to load
     * @throws IOException
     * @throws InterruptedException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    public InstrumentationHelper(File apkFile) throws IOException, InterruptedException, ParserConfigurationException,
            SAXException, XPathExpressionException {
        mainActivities = new HashSet<>();
        // unpack
        Log.debug(TAG, "Decoding " + apkFile.getAbsolutePath());
        String cmd = "java -jar tools/apktool.jar d -s -f " + apkFile.getAbsolutePath() + " -o decoded";
        Process p = Runtime.getRuntime().exec(cmd);
        int processExitCode = p.waitFor();
        if (processExitCode != 0) {
            throw new RuntimeException("Something went wrong during unpacking");
        }
        BufferedReader br = new BufferedReader(new FileReader("decoded/AndroidManifest.xml"));
        StringBuffer sb = new StringBuffer();
        String line = "";
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        br.close();
        manifest = sb.toString();

        // Do some XML parsing
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(manifest.getBytes()));
        doc.getDocumentElement().normalize();

        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr1 = xpath.compile("//manifest/@package");
        NodeList nodes = (NodeList)expr1.evaluate(doc, XPathConstants.NODESET);
        packagename = ((Attr) nodes.item(0)).getValue();

        xpath = XPathFactory.newInstance().newXPath();
        expr1 = xpath.compile("//manifest/application/activity[intent-filter/action[@name='android.intent.action.MAIN']]/@name");
        nodes = (NodeList)expr1.evaluate(doc, XPathConstants.NODESET);
        for (int i=0;i<nodes.getLength();i++) {
            Node n = nodes.item(i);
            String classname = ((Attr) n).getValue();
            if (classname.startsWith("."))
                classname = packagename + classname;
            mainActivities.add(classname);
        }
    }

    public SootMethod getDefaultOnResume() {
        assert mainActivities.size()>0:"No default activities in AndroidManifest.xml";

        SootClass mainAct = Scene.v().getSootClass(mainActivities.iterator().next());
        SootMethod onResume;
        try {
            onResume = mainAct.getMethod("void onResume()");
        } catch (RuntimeException rte) {
            onResume=null;
        }
        return onResume;
    }

    public SootMethod getDefaultOnCreate() {
        SootMethod defaultOnCreate = null;
        for (String mainAct:mainActivities) {
            if (Scene.v().containsClass(mainAct)) {
                SootClass mainClass = Scene.v().getSootClass(mainAct);
                if (mainClass.declaresMethod("void onCreate(android.os.Bundle)")) {
                    return mainClass.getMethod("void onCreate(android.os.Bundle)");
                }
            } else {
                throw new RuntimeException("Unexpected: Main activity class not present in Scene: " + mainAct);
            }
        }

        return defaultOnCreate;
    }
}
