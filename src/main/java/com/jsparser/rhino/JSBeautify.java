package com.jsparser.rhino;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONObject;

public class JSBeautify {

    public static void main(String[] args) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        String options = "{" +
                "  \"indent_size\": \"4\"," +
                "  \"indent_char\": \" \"," +
                "  \"max_preserve_newlines\": \"5\"," +
                "  \"preserve_newlines\": true," +
                "  \"keep_array_indentation\": true," +
                "  \"break_chained_methods\": true," +
                "  \"indent_scripts\": \"normal\"," +
                "  \"brace_style\": \"collapse,preserve-inline\"," +
                "  \"space_before_conditional\": true," +
                "  \"unescape_strings\": false," +
                "  \"jslint_happy\": false," +
                "  \"end_with_newline\": true," +
                "  \"wrap_line_length\": \"0\"," +
                "  \"indent_inner_html\": false," +
                "  \"comma_first\": true" +
                "}";

        String options2 = "{" +
                "    \"end_with_newline\": true," +
                "    \"jslint_happy\": true," +
                "    \"space_after_anon_function\": true" +
                "}";

        try {

            JSONObject json = new JSONObject(options2);
            engine.eval("var global = this;");
            engine.eval(new FileReader("beautify.js"));

            Invocable invocable = (Invocable) engine;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/tmp/upgraded/ezsource_1.4/src/main/webapp/app.js")));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            Map<String, Object> options1 = new HashMap<String, Object>() {
                {
                    put("end_with_newline", true);
                    put("jslint_happy", true);
                    put("space_after_anon_function", true);
                }
            };

            String result = (String)invocable.invokeFunction("js_beautify", sb.toString(), options1);
            System.out.println(result);

        } catch (ScriptException | NoSuchMethodException | IOException e) {
            e.printStackTrace();
        }

    }
}
