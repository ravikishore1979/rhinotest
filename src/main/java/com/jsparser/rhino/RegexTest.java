package com.jsparser.rhino;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class RegexTest {

    public String findAndReplace(Pattern pt, String searchStr, Function<Matcher, String> getReplaceString) {
        Matcher matcher = pt.matcher(searchStr);
        StringBuilder sb = new StringBuilder();
        int lastSearchIndex = 0;

        while(lastSearchIndex < searchStr.length() && matcher.find(lastSearchIndex)) {
            int start = matcher.start();
            String previousSubStr = searchStr.substring(lastSearchIndex, start);
            String replacedStr = getReplaceString.apply(matcher);
            sb.append(previousSubStr).append(replacedStr);
            lastSearchIndex = matcher.end();
        }
        sb.append(searchStr.substring(lastSearchIndex));
        return sb.toString();
    }

    public static void main(String args[]) {
        String searchStr = "sdfsdf   //\"use strict\"; adfd 343 sdfsdf  \"use strict\"; 3434s";
        String expected = "sdfsdf   //\"use strict\"; adfd 343 sdfsdf   3434s";
        Pattern useStrictPt = Pattern.compile("(\\/\\/)?\"use strict\";?");
        Function<Matcher, String> getReplaceString = (matcher) -> {
            if(StringUtils.isBlank(matcher.group(1))) {
                return "";
            }
            return matcher.group(0);
        };
        String newStr = new RegexTest().findAndReplace(useStrictPt, searchStr, getReplaceString);
        System.out.println("Migration status : [" + expected.equalsIgnoreCase(newStr) + "]  After Replacing: [" + newStr + "]");
    }


}
