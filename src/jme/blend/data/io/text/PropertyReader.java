/*
 * Copyright (c) 2017 Juraj Papp
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme.blend.data.io.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import jme.blend.data.io.text.Tokenizer.*;
import static jme.blend.data.io.text.Tokenizer.*;

/**
 *
 * @author Juraj Papp
 */
public class PropertyReader {
    public static final Tokenizer tokenizer;
    static {
        
        tokenizer = new Tokenizer();
        Cond bracketCond = new Cond() {
            public boolean is(String s, int f, int t) {
                s = s.substring(f,t);
                Sent string = tokenizer.sent.get("string");
                Token j;
                for(int i = 0; i < s.length(); i++) {
                        if((j = string.is(s, i, s.length())) != null) {
                                s = s.substring(0, i) + s.substring(j.to);
                        }
                }
                int k = 0;
                for(int i = 0; i < s.length(); i++)
                        if(s.charAt(i) == '{') k++;
                        else if(s.charAt(i) == '}') k--;
                return k == 0;
            }
        };
        Cond arrayCond = new Cond() {
            public boolean is(String s, int f, int t) {
                s = s.substring(f,t);
                Sent string = tokenizer.sent.get("string");
                Token j;
                for(int i = 0; i < s.length(); i++) {
                        if((j = string.is(s, i, s.length())) != null) {
                                s = s.substring(0, i) + s.substring(j.to);
                        }
                }
                int k = 0;
                for(int i = 0; i < s.length(); i++)
                        if(s.charAt(i) == '[') k++;
                        else if(s.charAt(i) == ']') k--;
                return k == 0;
            }
        };
        
        tokenizer.oneOf("separatorChar", ",;");
        tokenizer.add("separator", "separatorChar", AND);

        tokenizer.map("printable", new Syntax() {
            @Override
            public boolean is(char ch) {
                return !Character.isWhitespace(ch);
            }
        });
        tokenizer.map("whitespace", new Syntax() {
            @Override
            public boolean is(char ch) {
                return Character.isWhitespace(ch);
            }
        });
        tokenizer.add("space", "whitespace", TIMES_X);

        
        tokenizer.map("propNameLetter", new Syntax() {
            @Override
            public boolean is(char ch) {
                return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_';
            }
        });
        
        tokenizer.add("propName", "propNameLetter", AND, "propNameLetter", Tokenizer.TIMES_X);
        tokenizer.oneOf("curlyClose", "}");
        
        tokenizer.add("string", new Cond() {
            @Override
            public boolean is(String s, int f, int t) {
                return s.charAt(t-1) != '\\';
            }
        },
                      "\"", "any:"+ANY, "\"", AND);
        tokenizer.add("block", bracketCond, "space", AND, "{", "any:"+ANY, "}");
        
        tokenizer.add("propNameTerm", "propName", AND, "space", AND, "::", AND);
        tokenizer.add("propline", "name:propName", AND, "space", AND, "::", "any:"+ANY, "propNameTerm");
        
        tokenizer.add("propBlock", "name:propName", AND, "space", AND, "body:block");
        
        tokenizer.add("array", arrayCond, "[", "any:"+ANY, "]", AND);
        tokenizer.add("word", "printable", AND, "printable", TIMES_X, "whitespace");
        
        
    }

    public static interface PropertyParser<T> {
        public Map<String, T> createMap(String blockname);
        public T parse(String blockname, String propname, Object value);
    }
    public static HashMap<String, Map<String, Object>> parse(Path path, PropertyParser parser) throws IOException {
        return parse(new String(Files.readAllBytes(path)), parser);
    }
     
    
    public static Object parseValue(String text) {
        ArrayList<Token> tkns3 = tokenizer.parse(text, "array", "string", "separator", "word", "space");
        removeType(tkns3, "space");
        removeType(tkns3, "separator");
        
        if(tkns3.isEmpty()) return null;
        if(tkns3.size() > 1) {
            System.err.println("Syntax Error: " + text);
            return null;
        }
        return parseValue(tkns3.get(0));
    }
    static Object parseValue(Token t) {
        if(t.type.equals("array")) {
            ArrayList<Token> tkns = tokenizer.parse(t.get("any").toString(), "array", "string", "separator", "word", "space");
            removeType(tkns, "space");
            removeType(tkns, "separator");

            Object[] arr = new Object[tkns.size()];
            for(int i = 0; i < arr.length; i++)
                arr[i] = parseValue(tkns.get(i));
            return arr;
        } else if(t.type.equals("string")) {
            return t.get("any").toString();
        } if(t.type.equals("word")) {
            return t.toString().trim();
        } 
        return null;
    }
    static void removeType(ArrayList<Token> list, String type) {
        for(int i = 0; i < list.size(); i++)
            if(list.get(i).type.equals(type)) list.remove(i--);
    }
    public static HashMap<String, Map<String, Object>> parse(String text, PropertyParser parser) {
        HashMap<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>();
        
        
        
        ArrayList<Token> tkns = tokenizer.parse(text, "propBlock", "space");
//                System.out.println("tokens size " + tkns.size());

        for(Token t : tkns) {
            if(t.type.equals("propBlock")) {
                String name = t.get("name").toString().trim();
                
//                System.out.println("token " + t.type + ": " + t.toString());
                Token any = t.get("body").get("any");
//                System.out.println("    content: " + any.toString());
                ArrayList<Token> tkns2 = tokenizer.parse(any.toString(), "propline", "space");
                for(Token t2 : tkns2) {
                    if(t2.type.equals("propline")) {
                        String propname = t2.get("name").toString();
                        String propvalue = t2.get("any").toString();
                        
                        Object value = parseValue(propvalue);
                        
                        Map<String, Object> set = map.get(name);
                        if(set == null) {
                            set = parser == null ? new HashMap<String, Object>() : parser.createMap(name);
                            map.put(name, set);
                        }
                        
                        if(parser == null) {
                            set.put(propname, value);
                        }
                        else {
                            Object input = parser.parse(name, propname, value);
                            if(input != null) set.put(propname, input);
                        }
                    }
                }
            }
        }
        return map;
    }
}
