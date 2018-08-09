package com.jsparser.rhino;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.PropertyGet;

public class PrintNodes {
	  public static void main(String[] args) throws IOException {
	      //Testing using files
//          testFile("/tmp/upgraded/ezsource_1.4/src/main/webapp/pages/eSignDocs/eSignDocs.js");

          //Testing using strings
          /*
          testJSString("var a = c + d //test inline \n" +
                                "    +x; //llskd6\n", "static1");
          testJSString("var a; //aerf\n" +
                  "         var b //def\n" +
                  "         var c = a + b;\n", "static2");
          testJSString("f1(a,b) //asdlkj\n" +
                  "         f2(2,3);", "fcalltest");
          testJSString( "if(a == 2) //if1\n" +
                  "             c = 10 //then\n" +
                  "         else //else part\n" +
                  "             c = 5; //else parth\n" +
                  "         d = 34;\n", "iftest");
          testJSString("do //do\n" +
                  "           d = 3 + 3;\n" +
                  "         while(false); //End of do-while\n", "doTest");
          testJSString("do //do\n" +
                  "         {\n" +
                  "             d = 3 + 3;\n" +
                  "         } while(false); //End of do-while\n", "doBlockTest");
          testJSString("while(false) //while\n" +
                  "           d = 3 + 3;\n", "whileTest");
          testJSString("while(false) //while\n" +
                  "        {\n" +
                  "             d = 3 + 3;\n" +
                  "        }\n", "whileblockTest");
         testJSString("WM.element($event.target).parent().find('.edit-row-button').click();", "wmelement");
          testJSString("$scope.buttonHideChangeLogClick = function($event, $isolateScope) {\n" +
                          "        $scope.Variables.staticShowChangeLog.dataSet.dataValue = false;\n" +
                          "        $scope.Widgets.gridBidEditor.setProperty('height', \"700px\");\n" +
                          "        //$scope.Variables.Job1111ExecuteGetBidChangeLogByID.setInput('Id', 0);\n" +
                          "    };", "wmelement");

      testJSString("var timeRegistrationLines = {\n"+
                           "    \"orderId\": a.orderId,\n"+
                           "    \"registeredQuantity\": a.datavalue,\n"+
                           "    // \"surchargeComponent1\": (a.surchargeComponent1.datavalue.surchargeComponent || null),\n"+
                           "    // \"surchargeComponent2\": (a.surchargeComponent2.datavalue.surchargeComponent || null),\n"+
                           "    \"unit\": a.unit,\n"+
                           "    \"wageCompType\": a.wageCompType,\n"+
                           "    // \"wageCompSubType2\": (a.extraWageComponentSubType.datavalue || null),\n"+
                           "    \"week\": a[a.timeLineFrom].weekNumber,\n"+
                           "    \"comments\": (a.datavalue || null)\n"+
                           "};", "wmelement");

         */
//          testJSString("Variables['x'].y", "functest");
//          testJSString("a.b = x;", "functest");
          testJSString("Widgets.getProperty(x.y ? 'x' : ('y' + 34)) == 'sdf' ? Widgets.setProperty((x.y ? 'x' : ('y' + 34)), 'va1') : 'dfdf'", "functest");
//          testJSString("Widgets.widgetName.getProperty(x.y ? 'x' : ('y' + 34))", "t1");


          String input1 = "var xml = \"<xml>\" +\n" +
                  "            \"<endorsementRequest>\" +\n" +
                  "            \"<policyNo>\" + variable.activeScope.Widgets.policyNo.datavalue + \"</policyNo>\" +\n" +
                  "            \"<intimationDate>\" + variable.activeScope.Widgets.intimationDate.datavalue + \"</intimationDate>\" +\n" +
                  "            \"<effectiveFrom>\" + variable.activeScope.Widgets.effectiveFrom.datavalue + \"</effectiveFrom>\" +\n" +
                  "            \"<effectiveTo>\" + variable.activeScope.Widgets.effectiveTo.datavalue + \"</effectiveTo>\" +\n" +
                  "            \"<endorsementType>\" + variable.activeScope.Widgets.endorsementType.datavalue || 001 + \"</endorsementType>\" +\n" +
                  "            \"<endorsementRemarks>\" + variable.activeScope.Widgets.endorsementRemarks.datavalue + \"</endorsementRemarks>\" +\n" +
                  "            \"</endorsementRequest>\" +\n" +
                  "            \"</xml>\";";
//          testJSString(input1, "functest");
      }

    public static void testJSString(String s, String static1) throws IOException {
        String parsedJSStr = new PrintNodes().parseJSString(s, static1, null);
        System.out.println(parsedJSStr);
    }

    public static void testFile(String file) throws IOException {
        Reader reader = new FileReader(file);
        try {
          String parsedJSStr = new PrintNodes().parseJSString(null, file, reader);
        } finally {
             reader.close();
        }
    }

    public String parseJSString(String jsStringToParse, String jsName, Reader jsReader) throws IOException {
		  CompilerEnvirons env = new CompilerEnvirons();
		  env.setRecordingLocalJsDocComments(true);
		  env.setAllowSharpComments(true);
		  env.setRecordingComments(true);
		  env.setRecoverFromErrors(true);
		  env.setIdeMode(true);
		  env.setStrictMode(false);
		  env.setErrorReporter(new MyErrorReporter());

		  AstRoot node = null;
		  if(jsReader != null) {
		      node = new Parser(env).parse(jsReader, jsName, 1);
          } else {
		    node = new Parser(env).parse(jsStringToParse, jsName, 1);
          }
//	      node.visitAll(new Printer());
		  return node.debugPrint();

        /*FileReader reader = new FileReader("/tmp/upgraded/ezsource_1.4/src/main/webapp/pages/eSignDocs/eSignDocs_9_x.js");
        BufferedReader breader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = breader.readLine()) != null) {
            sb.append(line);
        }
        Map<String, List<String>> scopeMap = getControllerDefinedProperties(node, sb.toString());

		  return node.toSource();*/
	}


    private Map<String, List<String>> getControllerDefinedProperties(AstNode node, String oldScript) {
        Map<String, List<String>> scopeMap = new HashMap<>();
        final Pattern appController = Pattern.compile("Application\\.\\$controller\\(\"(.*?)\",");
        Matcher matcher = appController.matcher(oldScript);
        while (matcher.find()) {
            String controllerName = matcher.group(1);
            if(!controllerName.endsWith("PageController")) {
                controllerName = controllerName.substring(0, controllerName.indexOf("Controller"));
                scopeMap.put(controllerName, new ArrayList<>());
            }
        }

        Set<String> controllerNames = scopeMap.keySet();

        Node childNode = node.getFirstChild();
        childNode = childNode.getType() == Token.EXPR_RESULT ? childNode : childNode.getNext();
        while (childNode != null) {
            if(childNode.getType() == Token.EXPR_RESULT) {
                Node assignNode = ((ExpressionStatement) childNode).getExpression();
                if(assignNode.getType() == Token.ASSIGN) {
                    Node left = ((Assignment)assignNode).getLeft();
                    if(left.getType() == Token.GETPROP) {
                        PropertyGet pgNode = (PropertyGet) left;
                        if(pgNode.getRight().getType() == Token.NAME && pgNode.getLeft().getType() == Token.NAME) {
                            String leftIdentifier = ((Name)pgNode.getLeft()).getIdentifier();
                            String rightIdentifier = ((Name)pgNode.getRight()).getIdentifier();
                            if(leftIdentifier.equals("Page") || leftIdentifier.equals("Prefab") || leftIdentifier.equals("Partial")) {
                                 controllerNames.forEach(controller -> {
                                     if(rightIdentifier.startsWith(controller+"_")) {
                                         scopeMap.get(controller).add(rightIdentifier.substring(controller.length()+1));
                                     }
                                 });
                            }
                        }
                    }
                }
            }
                childNode = childNode.getNext();
        }
        return scopeMap;
    }


    class Printer implements NodeVisitor {
        String widgetName = "DepartmentTable1";
        @Override public boolean visit(AstNode node) {
            String indent = "%1$Xs".replace("X", String.valueOf(node.depth() + 1));
//	        System.out.println("**************");
//	        System.out.format(indent, "").println(node.getClass() + " === " + Token.typeToName(node.getType()) + " === Tree:\n" + node.debugPrint());

            if(node.getType() == Token.GETPROP) {
                PropertyGet pgNode = ((PropertyGet)node);
                if(pgNode.getLeft().getType() == Token.NAME) {
                    Name leftNameNode = (Name)pgNode.getLeft();
                    if("$scope".equalsIgnoreCase(leftNameNode.getIdentifier())) {
                        leftNameNode.setIdentifier("Page");

                        Name newWidgetNameNode = new Name(0, "DepartmentTable1");
                        PropertyGet newPgNode = new PropertyGet(leftNameNode, newWidgetNameNode);
                        pgNode.setLeft(newPgNode);
//	        			System.out.println(nameNode.getParent().toSource() + " ==== " + pgNode.getRight().toSource());
                    }
                    return false;
                }
            }
            if(node.getType() == Token.NAME) {
                Name nameNode = (Name)node;
                if("\\$scope".equalsIgnoreCase(nameNode.getIdentifier())) {
                    nameNode.setIdentifier("Page");
                }
                return false;
            }
            return true;
        }
    }

    private class MyErrorReporter implements ErrorReporter {

        @Override
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
            // TODO Auto-generated method stub
            System.out.println(message + "  " + lineSource + "  " + line + "  " + lineOffset);

        }

        @Override
        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource,
                                               int lineOffset) {
            // TODO Auto-generated method stub
            System.out.println(message + "  " + lineSource + "  " + line + "  " + lineOffset);
            return new EvaluatorException("Test error msg" + message);
        }

        @Override
        public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
            System.out.println(message + "  " + lineSource + "  " + line + "  " + lineOffset);

            // TODO Auto-generated method stub

        }
    }
}
