package com.jsparser.rhino;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptMigrator {
	private static final Logger logger = LoggerFactory.getLogger(ScriptMigrator.class);
	private static final List<String> elementsToCreateInjector = Arrays.asList("$q");
	private static final List<String> funcParamsToBeRemoved = Arrays.asList("$timeout", "$interval");

	private final JSPageType pageType;
	private Reader reader;
	private String fileName;
	private Map<String, String> replaceVarMap;

	public ScriptMigrator(Reader reader, String fileName, JSPageType pgType) {
		this.reader = reader;
		this.fileName = fileName;
		this.pageType = pgType;
		this.replaceVarMap = new HashMap<>();
		this.replaceVarMap.put("$scope", pageType.getPageType());
	}

	public String getMigratedScript() throws IOException {
		AstRoot node;
		ScriptNode destScriptNode = new AstRoot();
		try {
			CompilerEnvirons env = new CompilerEnvirons();
			env.setLanguageVersion(Context.VERSION_ES6);
			env.setRecordingLocalJsDocComments(true);
			env.setAllowSharpComments(true);
			env.setRecordingComments(true);
			env.setRecoverFromErrors(true);
			env.setAllowSharpComments(true);
			env.setRecordingLocalJsDocComments(true);
			env.setIdeMode(true);
			env.setErrorReporter(new RhinoErrorReporter());
			node = new Parser(env).parse(this.reader, this.fileName, 1);
		} catch (IOException e) {
			logger.error("Exceptioin while migrating the file [" + this.fileName + "]", e);
			throw e;
		}

		if (node != null) {
			AstNode childNode = (AstNode) node.getFirstChild();
			while (childNode != null) {
				AstNode currentNode = childNode;
				childNode = (AstNode) currentNode.getNext();
				int type = currentNode.getType();
				if (Token.EXPR_RESULT == type) {
					processExprResult((ExpressionStatement) currentNode, destScriptNode);
				} else {
					addToDestScript(currentNode, destScriptNode, "Statement is not of type function call");
				}
			}
			return destScriptNode.toSource();
		}
		return null;
	}

	private void processExprResult(ExpressionStatement childNode, ScriptNode destScriptNode) {
		AstNode expressionNode = childNode.getExpression();
		if (Token.CALL != expressionNode.getType()) {
			addToDestScript(childNode, destScriptNode, "Statement is not of type Function call");
			return;
		}
		FunctionCall fcNode = (FunctionCall) expressionNode;
		if (!("Application.$controller".equals(fcNode.getTarget().toSource()) || "Application.run".equals(fcNode.getTarget().toSource()))) {
			addToDestScript(childNode, destScriptNode, "Function call is not of type 'Application.$controller'");
			return;
		}
		List<AstNode> funcArgs = fcNode.getArguments();
		FunctionNode funcNode = null;
		ArrayLiteral arrayLitNode = null;
		String widgetName = null;
		AstNode possibleFuncNode;
		if (funcArgs.size() > 1) {
			if ("Application.$controller".equals(fcNode.getTarget().toSource())) {
				if (Token.STRING != funcArgs.get(0).getType()) {
					addToDestScript(childNode, destScriptNode, "first argument passed is not of type 'String literal'");
					return;
				}
				widgetName = ((StringLiteral) funcArgs.get(0)).getValue();

				if (!widgetName.contains("Controller")) {
					addToDestScript(childNode, destScriptNode, "first argument string does not contain 'Controller'");
					return;
				}
				widgetName = widgetName.substring(0, widgetName.indexOf("Controller"));
				widgetName = widgetName.contains("Page") ? null : widgetName;
			}
			possibleFuncNode = funcArgs.get(1);
		} else {
			possibleFuncNode = funcArgs.get(0);
		}

		if (possibleFuncNode.getType() == Token.ARRAYLIT) {
			arrayLitNode = (ArrayLiteral) possibleFuncNode;
			funcNode = (FunctionNode) arrayLitNode.getElement(arrayLitNode.getSize() - 1);
		} else {
			funcNode = (FunctionNode) possibleFuncNode;
		}

		if (funcNode == null) {
			addToDestScript(childNode, destScriptNode, "Last argument is not function definition");
			return;
		}

		if (arrayLitNode != null && arrayLitNode.getSize() > 1) {
			List<AstNode> paramNodeList = funcNode.getParams();
			List<AstNode> paramsToRemove = new ArrayList<>();
			for (int i = 0; i < paramNodeList.size(); i++) {
				AstNode litNode = arrayLitNode.getElement(i);
				String litStrName = ((StringLiteral) litNode).getValue();
				Name paramNode = (Name) paramNodeList.get(i);
				String functParamNameStr = paramNode.getIdentifier();
				switch (litNode.getType()) {
					case Token.STRING: {
						if ("$scope".equals(litStrName)) {
							if (!"$scope".equals(functParamNameStr)) {
								String oldScopeVal = this.replaceVarMap.get("$scope");
								this.replaceVarMap.put(functParamNameStr, StringUtils.isNotBlank(oldScopeVal) ? oldScopeVal : "Page");
							}
						} else if (elementsToCreateInjector.contains(litStrName)) {
							AstNode injectNode = createAppDependency(functParamNameStr);
							destScriptNode.addChildToFront(new EmptyStatement());
							destScriptNode.addChildToFront(injectNode);
						} else if(funcParamsToBeRemoved.contains(litStrName)) {
							paramsToRemove.add(paramNode);
							if(!litStrName.equalsIgnoreCase(functParamNameStr)) {
								this.replaceVarMap.put(functParamNameStr, NodeRefactor.replaceableVarMap.get(litStrName));
							}
						}
						break;
					}
					default: {
						if (litNode.getType() != Token.FUNCTION) {
							logger.info("Found type [{}] of paramName [{}] name other than String and Function.", litNode.getType(), litStrName);
						}
						break;
					}
				}
			}
			paramsToRemove.forEach(paramNode -> paramNodeList.remove(paramNode));
		}

		funcNode.visit(new NodeRefactor(widgetName, this.replaceVarMap));
		Block funcBlock = (Block) funcNode.getBody();
		moveAllStatementsOfToDest(funcBlock, destScriptNode);
	}

	private void addToDestScript(AstNode childNode, AstNode destScriptNode, String reason) {
		logger.info("Not migrating the code at line no [{}] of the original source. Due to [{}]", childNode.getLineno(), reason);
		destScriptNode.addChild(childNode);
	}

	private void moveAllStatementsOfToDest(Block funcBlock, ScriptNode destScriptNode) {
		AstNode childNode = (AstNode) funcBlock.getFirstChild();
		if ("\"use strict\";".equalsIgnoreCase(childNode.toSource().trim())) {
			childNode = (AstNode) childNode.getNext();
		}
		while (childNode != null) {
			AstNode currentNode = childNode;
			childNode = (AstNode) currentNode.getNext();
			if (currentNode.getType() == Token.EXPR_VOID && currentNode.toSource().trim().startsWith("$scope.ctrlScope = $scope")) {
				continue;
			}
			destScriptNode.addChild(currentNode);
		}
	}

	private AstNode createAndInsertInjector(String varStrName) {
		/*
		 * Creates variable declaration as below:
		 * let {varStrName} = injector.inject({varStrName});
		 */
		VariableDeclaration pn = new VariableDeclaration();
		pn.setType(Token.LET);
		Name varName = new Name(0, varStrName);

		VariableInitializer vi = new VariableInitializer();
		vi.setTarget(varName);
		vi.setType(Token.LET);
		pn.addVariable(vi);

		Name injectorName = new Name(0, "injector");
		Name methodName = new Name(0, "inject");
		PropertyGet pg = new PropertyGet(injectorName, methodName);
		//arguments
		StringLiteral sl = new StringLiteral();
		sl.setValue(varStrName);
		sl.setQuoteCharacter('"');

		FunctionCall fcall = new FunctionCall();
		fcall.setTarget(pg);
		fcall.addArgument(sl);

		vi.setInitializer(fcall);
		return pn;
	}

	private AstNode createAppDependency(String varStrName) {
		Name injectorName = new Name(0, "App");
		Name methodName = new Name(0, "getDependency");
		PropertyGet pg = new PropertyGet(injectorName, methodName);
		//arguments
		StringLiteral sl = new StringLiteral();
		sl.setValue(varStrName);
		sl.setQuoteCharacter('"');

		FunctionCall fcall = new FunctionCall();
		fcall.setTarget(pg);
		fcall.addArgument(sl);
		return fcall;
	}

	public static void main(String[] args) {
		String file = "/Users/ravik/wm/migration_ang_1_to_5/Main.js";
//		String file = "/Users/ravik/wm/migration_ang_1_to_5/tests/fail1.js";
//		String file = "/Users/ravik/wm/migration_ang_1_to_5/test1.js";
//      String file = "/Users/ravik/wm/migration_ang_1_to_5/tests/failSingleComment.js";
		try {
			System.out.println(new ScriptMigrator(new FileReader(new File(file)),
					file, JSPageType.PAGE).getMigratedScript());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}