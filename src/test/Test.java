package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfg.CFG;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;
import cfg.parse.LR0;
import cfg.parse.Parser;
import cfg.parse.SLR1;
import lexer.Lexer;
import lexer.Token;
import lexer.Word;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		//reader
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(
				new FileReader(
						new File(System.getProperty("user.dir"),"\\test_data\\pl0code_1.txt")));
		StringBuilder sb = new StringBuilder();
		String t;
		while((t = reader.readLine()) != null) {
			sb.append(t).append("\n");
		}
		//lexer
		Lexer lex = new Lexer(sb.toString());
		//stream
		List<Symbol> stream = new ArrayList<Symbol>();
		Token token=null;
		for(;(token=lex.scan())!=Word.over;) {
			stream.add(token.toSymbol());
		}
		//parser
			//CFG
		NontermianlSymbol A = new NontermianlSymbol("<assignment>");
		NontermianlSymbol E = new NontermianlSymbol("<expression>");
		NontermianlSymbol T = new NontermianlSymbol("<term>");
		NontermianlSymbol F = new NontermianlSymbol("<factor>");
		
		TerminalSymbol plus = Word.opSymbols.get(Word.opSymbols.indexOf(new TerminalSymbol("+")));
		TerminalSymbol minus = Word.opSymbols.get(Word.opSymbols.indexOf(new TerminalSymbol("-")));
		TerminalSymbol times = Word.opSymbols.get(Word.opSymbols.indexOf(new TerminalSymbol("*")));
		TerminalSymbol slash = Word.opSymbols.get(Word.opSymbols.indexOf(new TerminalSymbol("/")));
		TerminalSymbol lp = Word.delimeterSymbols.get(Word.delimeterSymbols.indexOf(new TerminalSymbol("(")));
		TerminalSymbol rp = Word.delimeterSymbols.get(Word.delimeterSymbols.indexOf(new TerminalSymbol(")")));
		TerminalSymbol id = Word.id;
		TerminalSymbol integer = lexer.Number.symbol;
		TerminalSymbol assign = Word.opSymbols.get(Word.opSymbols.indexOf(new TerminalSymbol(":=")));


		
		Rule r1 = new Rule(A, Arrays.asList(id,assign,E));
		Rule r2 = new Rule(E, Arrays.asList(E,plus,T));
		Rule r3 = new Rule(E, Arrays.asList(E,minus,T));
		Rule r10 = new Rule(E, Arrays.asList(T));
		Rule r4 = new Rule(T, Arrays.asList(T,times,F));
		Rule r5 = new Rule(T, Arrays.asList(T,slash,F));
		Rule r6 = new Rule(T, Arrays.asList(F));
		Rule r7 = new Rule(F, Arrays.asList(lp,E,rp));
		Rule r8 = new Rule(F, Arrays.asList(id));
		Rule r9 = new Rule(F, Arrays.asList(integer));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5,r6,r7,r8,r9,r10));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(A,E,T,F));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(plus,minus,times,slash,lp,rp,assign,id,integer));
		NontermianlSymbol startSymbol = A;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		//
		Parser parser = new SLR1(cfg);
		System.out.println(parser.parse(stream));;
	}

}
