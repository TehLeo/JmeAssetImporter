/*
 * Copyright (c) 2017, Juraj Papp
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

import java.util.ArrayList;
import java.util.HashMap;

//can add parsing ordered by smallest obtained token
public class Tokenizer {
	public static final String AND = "*AND*";
//	public static final String TIMES = "*TIMES*";
	public static final String TIMES_X = "*TIMES_X*";
	public static final String TERM = "*TERM*";
	public static final String ANY = "*ANY*";
	public interface Syntax {
		boolean is(char ch);
	}
	public interface Sent {
		public Token is(String s, int f, int t);
	}
	public class ASent implements Sent {
		String name;
		Cond[] cond;
		String[] ss;
		String[] map;
		public ASent(String name, Cond[] cond, String... ss) {
			this.name = name;
			this.cond = cond;
			
			map = new String[ss.length];
			for(int i = 0; i < ss.length; i++) {
				int index = ss[i].indexOf(NAME_SYMBOL);
				if(index != -1) {
					map[i] = ss[i].substring(0, index);
					ss[i] = ss[i].substring(index+1);
				}
			}
			
			this.ss = ss;
		}
                public int getTerminalIndex() {
//                    if((ss.length & 1) == 0 && !ss[ss.length-1].equals(TERM)) return ((ss.length-1)>>1);
                    for(int i = Math.min((ss.length-1)|1,ss.length-1); i >= 0; i -= 2)
                        if(!ss[i].equals(TERM)) return ((i-1)>>1);
                    return 0;
                }
//		public Token loop(String s, int i, int at, int t, byte flag) {
//			Token tkn = new Token(ss[i], s); tkn.from = at;
//			
//			Sent sss = sent.get(ss[i]);
//			 Syntax sntx;
//			 Token newAt;
//			point:
//				do {
//				if(sss == null) {
//					sntx = syntax.get(ss[i]);
//					if(sntx == null) {
//						if(s.length() < at+ss[i].length()) {
//							if(flag == 1) break point; else return null;
//						}
//						for(int j = 0; j < ss[i].length(); j++)
//							if(s.charAt(at+j) != ss[i].charAt(j)) {
//								if(flag == 1) break point; else return null;
//							}
//						if(flag != 2) at += ss[i].length();
//					}
//					else {
//						if(!sntx.is(s.charAt(at))) {
//							if(flag == 1) break point; else return null;
//						}
//						if(flag != 2) at++;
//					}
//				}
//				else {
//					if((newAt = sss.is(s, at, t)) != null) {
//						at = newAt.to;
//						tkn = newAt;
//						if(at == t) break point;
//						if(flag == 1) continue; else break point;
//					}
//					if(flag == 1) break point; else  return null;
//				}
//					if(at == t) 
//						break point;
//				} while(flag == 1);
//			tkn.to = at;
//			
//			return tkn;
//		}
		public Token loop(String s, int i, int at, int t, byte flag) {
			Token tkn = new Token(ss[i], s); tkn.from = at;
			Sent sss = sent.get(ss[i]);
			Syntax sntx;
			Token newAt;
			if(sss == null) {
				sntx = syntax.get(ss[i]);
				if(sntx == null) {
					if(s.length() < at+ss[i].length()) 
						return null;
					for(int j = 0; j < ss[i].length(); j++)
						if(s.charAt(at+j) != ss[i].charAt(j)) 
							return null;	
					if(flag != 2) at += ss[i].length();
				}
				else {
					if(!sntx.is(s.charAt(at))) 
						return null;
					if(flag != 2) at++;
				}
			}
			else {
				if((newAt = sss.is(s, at, t)) != null) {
					at = newAt.to;
					tkn = newAt;
				}
				else return null;
			}
			tkn.to = at;	
			return tkn;
		}
		public byte getFlag(int i) {
			byte flag = 2;
			if(i != ss.length) {
				if(ss[i].equals(AND)) 
					flag = 0;
				else if(ss[i].equals(TIMES_X))
					flag = 1;
				else if(ss[i].equals(TERM))
					flag = 2;
				else if(ss[i].equals(ANY))
					flag = 3;
			}
			return flag;
		}
		public Token is(String s, int f, int t) {
			Token tok = new Token(name, s); tok.from = f;

			int condC = -1;
			int at = f;
			byte flag = 0;
			boolean endCond = false;
			for(int i = 0; i < ss.length; i += 2) {
				flag = getFlag(i+1);
				endCond = true;
				for(int j = i; j < ss.length; j += 2) {
                                    int ff = getFlag(j+1);
                                    if(ff == 0 || ff == 3) {
                                        endCond = false;
                                        break;
                                    }
				}
				if(at == t) {
                                    if(endCond) {			
                                        tok.to = at;
                                        for(int j = i>>1; j < tok.tokens.length; j++)
                                            if(tok.tokens[j] == null) {
                                                    //tok.tokens[j] = new Token(ss[ss.length-1-(ss.length+1)%2], s);
                                                    tok.tokens[j] = new Token(0, ((j<<1)+1 < ss.length)?ss[j*2+1]:TERM, s);
                                                    tok.tokens[j].from = (j==0)?f:tok.tokens[j-1].to;
                                                    tok.tokens[j].to = at;
                                                    if(map[j<<1] != null) 
                                                            tok.map(map[j<<1], tok.tokens[j]);

                                            }
//                                        System.out.println("TOKEN END ");
                                        return tok;
                                    }
                                        
                                    return null; // MAybe
				}
				//System.out.println(s+">"+f+","+at+"; "+ss[i] + " flag " + flag + ";  " + tok.tokens.length + ", " + i);
				
				if(flag == 1) {
					int startat = at;
					ArrayList<Token> tempList = new ArrayList<Token>();
					Token token;
					while(at != t && (token = loop(s, i, at, t, flag)) != null) {
						if(!tempList.isEmpty() && token.to == at) break;
						tempList.add(token);
						at = token.to;
					}
					Token newt = new Token(tempList.size(), s);
					newt.from = startat;
					newt.to = at;
					for(int j = 0; j < tempList.size(); j++)
						newt.tokens[j] = tempList.get(j);		
					
					tok.tokens[i>>1] = newt;
				}
				else if((tok.tokens[i>>1] = loop(s, i, at, t, flag)) == null) {
					return null;
				}
				at = tok.tokens[i>>1].to;
				if(map[i] != null) 
					tok.map(map[i], tok.tokens[i>>1]);
				
				
				point:
				if(flag == 3) {
					condC++;
					Token newTkn;
					for(int j = at; j < t; j++) {
						if((newTkn = loop(s, i+2, j, t, flag)) != null) {//OR is it getFlag(i+2)
							if(cond != null && !cond[condC].is(s, at, j)) 
								continue;
							at = newTkn.to;
							tok.tokens[(i>>1)+1] = newTkn;
							if(map[i+1] != null) {
								Token any = new Token(ANY, s);
								any.from = tok.tokens[(i>>1)].to;
								any.to = newTkn.from;
                                                                

								tok.map(map[i+1], any);
							}
							i +=2;
							break point;
						}
					}
					if(getFlag(i+3) == 2) {
						if(cond != null && !cond[condC].is(s, at, t)) 
							return null;
						newTkn = new Token(ANY, s);
						newTkn.from = tok.tokens[(i>>1)].to;
						newTkn.to = t;
						at = t;
						tok.tokens[(i>>1)+1] = newTkn;
						if(map[i+1] != null) {
//							Token any = new Token(ANY, s);
//							any.from = tok.tokens[(i>>1)].to;
//							any.to = newTkn.from;
//							tok.map(map[i+1], any);

                                                        
							tok.map(map[i+1], newTkn);
						}
						i +=2;
						break point;
					}
					return null;
				}
			}
			tok.to = at;
			return tok;
		}
	}
	public interface Cond {
		boolean is(String s, int f, int t);
	}
	public class Token {
		public String type, text;
		public int from;
		public int to;
		public Token tokens[];
		public HashMap<String, Token> map;
		public Token(int size, String text) {
			this.type = TIMES_X;
			this.text = text;
			tokens = new Token[size];
		}
		public Token(String type, String text) {
			this.type = type;
			this.text = text;
			Sent s = sent.get(type);
			if(s != null && s instanceof ASent) {
				ASent as = (ASent)s;
				tokens = new Token[(as.ss.length>>1)+as.ss.length%2];
				return;
			}
			tokens = new Token[1];	
		}
		Token(int size, String type, String text) {
			this.type = type;
			this.text = text;
			tokens = new Token[size];
		}
		public void map(String s, Token v) {
			if(map == null) map = new HashMap<String, Token>();
//			System.out.println("MApping " + s + ":" + v);
			map.put(s, v);
		}
		public Token get(String s) {
			return map.get(s);
		}
		public String toString() {
			return text.substring(from, to);
		}
		public String getText(String text) {
			return text.substring(from, to);
		}
	}
	public char NAME_SYMBOL = ':';
	HashMap<String, Syntax> syntax = new HashMap<String, Tokenizer.Syntax>();
	public HashMap<String, Sent> sent = new HashMap<String, Tokenizer.Sent>();
	public void map(String name, Syntax s) {
		syntax.put(name, s);
	}
	public void oneOf(String name, final String oneOf) {
		syntax.put(name, new Syntax() {
			public boolean is(char ch) {
				for(int i = 0; i < oneOf.length(); i++)
					if(oneOf.charAt(i) == ch)
						return true;
				return false;
			}
		});
	}
	public void mapOf(String name, final String... map) {
		sent.put(name, new Sent() {
			public Token is(String s, int f, int t) {
				String a = s.substring(f, t);
				for(String m : map)
					if(a.startsWith(m)) {
						Token tt = new Token(0, m, s);
						tt.from = f;
						tt.to = f+m.length();
						return tt;
					}
				return null;
			}
		});
	}
	public void map(String name, final String... ss) {
		syntax.put(name, new Syntax() {
			public boolean is(char ch) {
				for(String s : ss) 
					if(syntax.get(s).is(ch))
						return true;
				return false;
			}
		});
	}
	
	public void add(String name, final String... ss) {
		add(name, (Cond[])null, ss);
	}
	public void add(String name, final Cond cond, final String... ss) {
		add(name, new Cond[] {cond}, ss);
	}
	public void add(final String name, final Cond[] cond, final String... ss) {
            ASent s;
            sent.put(name, s=new ASent(name, cond, ss));
//            System.out.println("ASent: " + name + ", " + s.getTerminalIndex());
        }
	public ArrayList<Token> parse(String text, String... order) {
		return parse(text, 0, text.length(), order);
	}
	public ArrayList<Token> parse(String text, int f, int to, String... order) {
		ArrayList<Token> tkns = new ArrayList<Tokenizer.Token>();
		int last = f;
		int curr = f;
		while(curr < to) {
			for(int i = 0; i < order.length; ) {
                                Sent s = sent.get(order[i]);
				Token t = s.is(text, curr, to);
				if(t != null) {
//                                        System.out.println("TOKEN " + text.substring(curr, Math.min(curr+3, text.length())));

					tkns.add(t);
					if(curr == t.to)
						curr++;
                                        else {
                                            if(s instanceof ASent) curr = t.tokens[((ASent)s).getTerminalIndex()].to;
                                            else
                                                curr = t.to;
                                        }
                                        i = 0;
					continue;
				}
				i++;
				if(curr >= to) {
					return tkns;
				}
			}	
			if(curr == last) {
				System.out.println("TOO SOON");
				return tkns;
			}
			last = curr;
		}
		return tkns;
	}
//	public static void main(String[] args) {
//		final Tokenizer tkn = new Tokenizer();
//		tkn.map("letter", new Syntax() {
//			public boolean is(char ch) {return Character.isLetter(ch);}});
//		tkn.map("digit", new Syntax() {
//			public boolean is(char ch) {return Character.isDigit(ch);}});
//		tkn.map("letOrDig", new Syntax() {
//			public boolean is(char ch) {return Character.isLetterOrDigit(ch);}});
//		tkn.oneOf("bracket", "{[()]}");
//		tkn.oneOf("operator", "=+-*/><");
//		tkn.map("whitespace", new Syntax() {
//			public boolean is(char ch) {return Character.isWhitespace(ch);}});
//		tkn.map("separator",  new Syntax() {
//			public boolean is(char ch) {return Character.isWhitespace(ch)||ch==',';}});
//		tkn.map("terminator", "separator", "bracket");
//		
//		Cond paramCond = new Cond() {
//			public boolean is(String s, int f, int t) {
//				s = s.substring(f,t);
//				Sent string = tkn.sent.get("string");
//				Token j;
//				for(int i = 0; i < s.length(); i++) {
//					if((j = string.is(s, i, s.length())) != null) {
//						s = s.substring(0, i) + s.substring(j.to);
//					}
//				}
//				int k = 0;
//				for(int i = 0; i < s.length(); i++)
//					if(s.charAt(i) == '(') k++;
//					else if(s.charAt(i) == ')') k--;
//				return k == 0;
//			}
//		};
//		Cond bracketCond = new Cond() {
//			public boolean is(String s, int f, int t) {
//				s = s.substring(f,t);
//				Sent string = tkn.sent.get("string");
//				Token j;
//				for(int i = 0; i < s.length(); i++) {
//					if((j = string.is(s, i, s.length())) != null) {
//						s = s.substring(0, i) + s.substring(j.to);
//					}
//				}
//				int k = 0;
//				for(int i = 0; i < s.length(); i++)
//					if(s.charAt(i) == '{') k++;
//					else if(s.charAt(i) == '}') k--;
//				return k == 0;
//			}
//		};
//		Cond semiCond = new Cond() {
//			public boolean is(String s, int f, int t) {
//				s = s.substring(f,t+1);
//				Sent string = tkn.sent.get("string");
//				Token j;
//				for(int i = 0; i < s.length(); i++) {
//					if((j = string.is(s, i, s.length())) != null) {
//						s = s.substring(0, i) + s.substring(j.to);
//					}
//				}
//				return s.charAt(s.length()-1) == ';';
//			}
//		};
//		
//		tkn.add("space", "whitespace", TIMES_X);
//		tkn.add("number", "digit", TIMES_X, "terminator");
//		tkn.add("string", new Cond() {
//			public boolean is(String s, int f, int t) {
//				return s.charAt(t-1) != '\\';
//			}
//		}, "\"", ANY, "\"", AND, "terminator");
//		tkn.add("word", "letter", AND, "letOrDig", TIMES_X, "terminator");
//		tkn.add("block", bracketCond, "space", AND, "{", ANY, "}", AND, "terminator");
//		tkn.add("class", bracketCond,"word", AND, "block");
//		tkn.add("method", paramCond, "name:word", AND, "space", AND, "(", "args:"+ANY, ")", AND, "block");
//		
//		String text = " wergwerg ".trim();
//		Token t = tkn.parse(text, "word").get(0);
//		System.out.println("> " + ((t==null)?"null":t.to) + ", " + t.type);
//		if(t != null) {
//			System.out.println(t.tokens[1].tokens[2]);
//		}
//	}
	public static void printToken(Token t, String tab) {
		if(t == null) {System.out.println(tab+"null"); return;}
		else System.out.println(tab+t+"["+t.from+","+t.to+"] ["+t.type + ":" + t.tokens.length+"]");
		for(int i = 0; i < t.tokens.length; i++)
			printToken(t.tokens[i],tab+"--|");
	}
}
