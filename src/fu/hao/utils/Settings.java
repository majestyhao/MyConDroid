/*
* Copyright 2016 Hao Fu and contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* Contributors:
*   Hao Fu
*/

package fu.hao.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Class: Settings
 * Description:
 * Authors: Hao Fu(haofu@ucdavis.edu)
 * Date: 7/14/2016 6:00 PM
 */
public class Settings {
	private static boolean debug = true;
	private static int logLevel = Log.MODE_MSG;
	private static Set<String> targetMethods = new HashSet<String>();

	private static String appDirectory = "";
	private static String appName = "Unknown";
	private static String outputDirectory = "output";
	private static String androidLib = "";

	private static boolean printOutput = true;
	private static boolean printConstraints = false;
	private static boolean generateStats = false;

	public static Set<String> getTargetMethods() {
		return targetMethods;
	}

	public static void setTargetMethods(Set<String> targetMethods) {
		Settings.targetMethods = targetMethods;
	}

	public static String getAppDirectory() {
		return appDirectory;
	}

	public static void setAppDirectory(String appDirectory) {
		Settings.appDirectory = appDirectory;
	}

	public static String getAppName() {
		return appName;
	}

	public static void setAppName(String appName) {
		Settings.appName = appName;
	}

	public static String getOutputDirectory() {
		return outputDirectory;
	}

	public static void setOutputDirectory(String outputDirectory) {
		Settings.outputDirectory = outputDirectory;
	}

	public static int getLogLevel() {
		return logLevel;
	}

	public static void setLogLevel(int logLevel) {
		Settings.logLevel = logLevel;
	}

	public static boolean getGenerateStats() {
		return generateStats;
	}

	public static void setAndroidLib(String androidLib) {
		Settings.androidLib = androidLib;
	}

	public static String getAndroidLib() {
		return androidLib;
	}

	private enum TargetType {
		METHODS,
		NATIVE,
		REFLECTION
	}

	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(boolean debug) {
		Settings.debug = debug;
	}

	static public TargetType Target = TargetType.METHODS;
	static public Set<String> TargetMethods = new HashSet<String>();

	static public String AppDirectory = null;
	static public String AppName = null;
	static public String OutputDirectory = null;

	static public boolean PrintOutput = true;
	static public boolean PrintConstraints = false;
	static public boolean GenerateStats = false;

}
