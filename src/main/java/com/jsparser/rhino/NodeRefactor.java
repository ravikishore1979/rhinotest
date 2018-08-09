package com.jsparser.rhino;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.PropertyGet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class NodeRefactor implements NodeVisitor {

	public static final Map<String, String> replaceableVarMap = new HashMap<String, String>() {
		private static final long serialVersionUID = -3264996078402893972L;

		{
			put("$scope", "Page");
			put("$rootScope", "App");
			put("$isolateScope", "widget");
			put("$timeout", "setTimeout");
			put("$interval", "setInterval");
		}
	};
	private static final Map<String, String> loginWidgetReplaceableVarMap = new HashMap<String, String>() {
		{
			put("usernametext", "j_username");
			put("passwordtext", "j_password");
			put("remembermecheck", "j_rememberme");
		}
	};

	private static final List<String> varsToWhichWidgetNameRequired = Arrays.asList("Page", "Partial", "Prefab");
	private static final List<String> scopeVarNotToBeReplaced = Arrays.asList("Widgets", "Variables", "Actions", "ctrlScope");

	private String widgetName;
	private Map<String, String> nodeSpecificReplacableVarMap;

	private static Pattern rootInSubObjectPattern = Pattern.compile("(Page|Partial|Prefab|widget)\\.\\$root(\\.|$)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	private static Pattern loginWidgetNames = Pattern.compile("Widgets\\.(usernametext|passwordtext|remembermecheck)(\\.|$)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);


	public NodeRefactor(String wname, Map<String, String> varMapForReplacing) {
		this.widgetName = wname;
		//Do not change the order as Map that is passed to the constructor should take higher precedence.
		this.nodeSpecificReplacableVarMap = new HashMap<>(replaceableVarMap);
		if (varMapForReplacing != null) {
			this.nodeSpecificReplacableVarMap.putAll(varMapForReplacing);
		}
	}

	@Override
	public boolean visit(AstNode node) {

		if (node.getType() == Token.EXPR_VOID) {
			if (node.toSource().trim().equals("$scope.ctrlScope = $scope;")) {
				return false;
			}
		}
		if (node.getType() == Token.GETPROP) {
			PropertyGet pgNode = ((PropertyGet) node);
			if (pgNode.getLeft().getType() == Token.NAME) {
				Name leftnameNode = (Name) pgNode.getLeft();
				if (this.nodeSpecificReplacableVarMap.containsKey(leftnameNode.getIdentifier())) {
					String replacedName = this.nodeSpecificReplacableVarMap.get(leftnameNode.getIdentifier());
					leftnameNode.setIdentifier(replacedName);
					if (rootInSubObjectPattern.matcher(pgNode.toSource()).find()) {
						replaceWithApp(pgNode);
					} else if (varsToWhichWidgetNameRequired.contains(replacedName)) {
						appendSubObjectWithWidgetName(pgNode);
					}
				} else if ("WM".equals(leftnameNode.getIdentifier())) {
					checkForWMElement(pgNode);
				}
				return false;
			}
			if(loginWidgetNames.matcher(pgNode.toSource()).find()) {
				traverseAndReplaceWidgetNames(pgNode);
			}
		}

		if (node.getType() == Token.NAME) {
			Name nameNode = (Name) node;
			if (this.nodeSpecificReplacableVarMap.containsKey(nameNode.getIdentifier())) {
				String replacedName = this.nodeSpecificReplacableVarMap.get(nameNode.getIdentifier());
				nameNode.setIdentifier(replacedName);
				if (StringUtils.isNotBlank(this.widgetName) && varsToWhichWidgetNameRequired.contains(replacedName)) {
					//Don't how this is working.
					nameNode.setIdentifier(replacedName + "." + this.widgetName);
				}
			}
			return false;
		}

		if(node.getType() == Token.COMMENT) {
			Comment commentNode = (Comment) node;
			processCommentText(commentNode);
		}
		return true;
	}

	private void processCommentText(Comment commentNode) {

		List<String> searchList = new ArrayList<>(Arrays.asList("$scope.$root", "$isloateScope.$root"));
		List<String> replacementList = new ArrayList<>(Arrays.asList("App", "App"));
		this.nodeSpecificReplacableVarMap.forEach((k,v) -> {
			searchList.add(k);
			replacementList.add(v);
		});

		String newCommentValue = StringUtils.replaceEachRepeatedly(commentNode.getValue(), searchList.toArray(new String[0]), replacementList.toArray(new String[0]));
		commentNode.setValue(newCommentValue);
	}

	private void appendSubObjectWithWidgetName(PropertyGet pgNode) {
		if (StringUtils.isNotBlank(this.widgetName)) {
			AstNode rightNode = pgNode.getRight();
			if (rightNode != null) {
				switch (rightNode.getType()) {
					case Token.GETPROP: {
						Name ltNode = (Name) ((PropertyGet) rightNode).getLeft();
						if (!scopeVarNotToBeReplaced.contains(ltNode.getIdentifier())) {
							ltNode.setIdentifier(this.widgetName + "_" + ltNode.getIdentifier());
						}
						break;
					}
					case Token.NAME: {
						Name rgName = (Name) rightNode;
						if (!scopeVarNotToBeReplaced.contains(rgName.getIdentifier())) {
							rgName.setIdentifier(this.widgetName + "_" + rgName.getIdentifier());
						}
						break;
					}
				}
			} else {
				//Check in which scenario this case will occur.
			}
		} else {
			//In case of pageController, onPageReady should be replaced by onReady
			AstNode rightNode = pgNode.getRight();
			if (rightNode.getType() == Token.NAME) {
				Name rgNameNode = (Name) rightNode;
				if (rgNameNode.getIdentifier().equals("onPageReady")) {
					rgNameNode.setIdentifier("onReady");
				}
			}
		}
	}

	private void traverseAndReplaceWidgetNames(AstNode node) {
		if(node == null) {
			return;
		}
		if(node.getType() == Token.NAME) {
			Name widgetNameNode = (Name) node;
			if(loginWidgetReplaceableVarMap.containsKey(widgetNameNode.getIdentifier())) {
				widgetNameNode.setIdentifier(loginWidgetReplaceableVarMap.get(widgetNameNode.getIdentifier()));
			}
			return;
		}
		if(node.getType() == Token.GETPROP) {
			PropertyGet pgNode = (PropertyGet) node;
			traverseAndReplaceWidgetNames(pgNode.getLeft());
			traverseAndReplaceWidgetNames(pgNode.getRight());
		}
	}


	private void replaceWithApp(PropertyGet pgNode) {
		AstNode rightNode = pgNode.getRight();
		((Name) pgNode.getLeft()).setIdentifier("App");
		if (rightNode != null) {
			switch (rightNode.getType()) {
				case Token.GETPROP: {
					Name rootRtNode = (Name) ((PropertyGet) rightNode).getRight();
					pgNode.setRight(rootRtNode);
					break;
				}
				case Token.NAME: {
					//Could be on the right operand of =, so take action based on parent Node type.
					AstNode parentOfPgNode = pgNode.getParent();
					switch (parentOfPgNode.getType()) {
						case Token.GETPROP: {
							PropertyGet parentPg = (PropertyGet) parentOfPgNode;
							parentPg.setLeft(pgNode.getLeft());
							break;
						}
					}
					break;
				}
			}
		} else {
			//Check in which scenario this case will occur.
		}
	}

	private void checkForWMElement(PropertyGet pgNode) {
		Name leftNameNode = (Name) pgNode.getLeft();
		AstNode rgNode = pgNode.getRight();
		if (Token.NAME == rgNode.getType() && "WM.element".equals(pgNode.toSource())) {
			AstNode parentNode = pgNode.getParent();
			if (Token.CALL == parentNode.getType()) {
				FunctionCall fcallNode = (FunctionCall) parentNode;
				fcallNode.setTarget(leftNameNode);
				leftNameNode.setIdentifier("$");
			}
		}
	}
}