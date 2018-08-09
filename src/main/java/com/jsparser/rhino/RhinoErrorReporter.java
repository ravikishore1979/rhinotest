package com.jsparser.rhino;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhinoErrorReporter implements ErrorReporter {
	public static final Logger logger = LoggerFactory.getLogger(RhinoErrorReporter.class);
	@Override
	public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
		logger.warn(String.format("Message [%s] from sourceName [%s] at Line [%d] script causing error [%s] at LineOffset [%d]", message, sourceName, line, lineSource, lineOffset));
	}

	@Override
	public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
		logger.error(String.format("Message [%s] from sourceName [%s] at Line [%d] script causing error [%s] at LineOffset [%d]", message, sourceName, line, lineSource, lineOffset));
	}

	@Override
	public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
		logger.error(String.format("Message [%s] from sourceName [%s] at Line [%d] script causing error [%s] at LineOffset [%d]", message, sourceName, line, lineSource, lineOffset));
		return new EvaluatorException("Test error msg" + message);
	}

}
