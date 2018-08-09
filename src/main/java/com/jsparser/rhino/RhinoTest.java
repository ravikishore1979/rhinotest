package com.jsparser.rhino;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ErrorCollector;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhinoTest {
	public static final Logger logger = LoggerFactory.getLogger(RhinoTest.class);
	public static Pattern appControllerPt = null;
	
	static {
//		appControllerPt = Pattern.compile("(Application\\.[\\n\\s]*\\$controller\\((\\\".*?\\\"),)");
//		appControllerPt = Pattern.compile("((Application\\.[\\n\\s]*\\$controller\\((\\\"(.*?)\\\"))(.*?\\][\\n\\s]*\\);))", Pattern.MULTILINE | Pattern.DOTALL);
		appControllerPt = Pattern.compile("(Application\\.[\\n\\s]*\\$controller\\(\\\"(.*?)\\\".*?\\][\\n\\s]*\\);)", Pattern.MULTILINE | Pattern.DOTALL);
	}
	
	public static void main(String[] args) {
		try {
//			new RhinoTest().parseScript("/Users/ravik/wm/migration_ang_1_to_5/Main.js");
			new RhinoTest().parseScript("/Users/ravik/wm/migration_ang_1_to_5/test1.js");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void parseScript(String scriptFile) throws IOException {
		
		Map<String, String> controllerMap = new HashMap<String, String>();
		String toMigrateScript = getContentsOfFile(scriptFile);
		if(toMigrateScript == null || toMigrateScript.isEmpty()) {
    		logger.error("Script to migrate is empty.");
    		return;
    	}
    	Matcher appMatcher = appControllerPt.matcher(toMigrateScript);
    	
    	while(appMatcher.find()) {
    		controllerMap.put(appMatcher.group(2), appMatcher.group(1));
    	}
    	if(controllerMap.size() == 0) {
    		logger.error("Failed to get Application.$controller method calls, using regular expression [" + appControllerPt.pattern() + "]");
    		return;
    	}
    	
    	for (Entry<String, String> controllerMethodObj : controllerMap.entrySet()) {
    		String controllerName = controllerMethodObj.getKey();
    		String methodCallStr = controllerMethodObj.getValue();
    		String widgetName = controllerName.substring(0, controllerName.indexOf("Controller"));
    		AstRoot astRoot = getParsedScriptRoot(methodCallStr, widgetName);
    		
//    		Node funcDefRoot = srcCompiler.getRoot().getLastChild().getFirstFirstChild().getFirstChild().getChildAtIndex(2).getSecondChild();
    		
    		System.out.println("Input Tree");
    		Node n = astRoot.getFirstChild();
    		
//    		((ExpressionStatement)((org.mozilla.javascript.ast.Block)((org.mozilla.javascript.ast.FunctionNode)((org.mozilla.javascript.ast.ArrayLiteral)((org.mozilla.javascript.ast.FunctionCall)((ExpressionStatement)n).getExpression()).getArguments().get(1)).getElement(1)).getBody()).getFirstChild().getNext().getNext()).getExpression()
    		//Application.$controller("MainPageController", ["$scope", function ($scope) {
    		FunctionNode fnNode = (FunctionNode)((ArrayLiteral)((FunctionCall)((ExpressionStatement)n).getExpression()).getArguments().get(1)).getElement(1);
    		System.out.println(fnNode.toSource());
    		displayComments(fnNode);
//    		System.out.println("Is function node [" + funcDefRoot.isFunction() + "]\n" + funcDefRoot.toStringTree());
//    		rewriteMethodDefinitions(funcDefRoot, destCompiler.getRoot().getLastChild());
//    		System.out.println(astRoot.debugPrint());
//    		displayComments(astRoot);
    		
//    		System.out.println(srcCompiler.getComments(widgetName));
		}
    	
    	System.out.println("Result Tree");
//    	System.out.println(destCompiler.toSource());
	}

	private void displayComments(AstNode astNode) {
		Set<Comment> commentset = astNode.getAstRoot().getComments();
		if(commentset != null && commentset.size() > 0) {
			for (Comment comm : commentset) {
					System.out.println(comm.toSource());
//					System.out.println(comm.getEnclosingScope().toSource());
//					System.out.println(comm.getValue());
			}
		}
	}
	
	private AstRoot getParsedScriptRoot(String scriptStr, String sourceName) {
		CompilerEnvirons compileEnv =  new CompilerEnvirons();
		compileEnv.setErrorReporter(new ErrorCollector());
		compileEnv.setRecordingComments(true);
		compileEnv.setRecoverFromErrors(true);
		compileEnv.setStrictMode(false);
		compileEnv.setOptimizationLevel(0);
		compileEnv.setRecoverFromErrors(true);
		
/*		Parser p = new Parser(compileEnv);
		return p.parse(scriptStr, sourceName, 0);*/
		
		IRFactory irf = new IRFactory(compileEnv);
		return irf.parse(scriptStr, sourceName, 0);
	}
	
	
	private String getContentsOfFile(String filePath) throws IOException {
		FileReader fr = new FileReader(filePath);
		BufferedReader br = null;
		StringBuilder sb;
		try {
			br = new BufferedReader(fr);
			sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			} 
		} finally {
			br.close();
		}
		return sb.toString();
	}

}
