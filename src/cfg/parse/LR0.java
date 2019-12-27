package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import cfg.CFG;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;

/**
 * LR分析法的归约过程是规范推导的逆过程（最左归约），
 * 所以LR分析过程是一种规范归约过程
 * 
 * 其中LR(0)分析器是在分析过程中不需向右查看输入符号，
 * 因而它对文法的限制较大，但它是构造其它LR类分析器的基础。

 * @author 90946
 *
 */
public class LR0 extends LR {
	public LR0(CFG cfg) {
		super(cfg);
		makeDfa();
		isLR0();
		makeLR0Table();
	}

	
	//构造LR0分析表
	private void makeLR0Table() {
		// TODO Auto-generated method stub
		ACTION = new String[states.size()][cfg.getTerminals().size()];
		GOTO = new String[states.size()][cfg.getNonterminals().size()];
		
		//根据起始项目构造接受项目
		List<Symbol> rList = new ArrayList<>(startRule.getRight());
		rList.add(delimiter);
		Rule accRule = new Rule(startRule.getLeft(), rList);
				
		for(Node state : states) {
			//规约项目
			Rule first = state.rules.get(0);
			if(state.rules.size() == 1 
					&& first.getRight().lastIndexOf(delimiter) == first.getRight().size()-1) {
				if(first.equals(accRule)) {
					//接受项目
					ACTION[state.state][cfg.getTerminals().indexOf(CFG.over)] = "acc";
				}else {
					//规约项目
					//Ri，i表示根据第几条规则规约，在这由CFG的规则顺序决定
					List<Symbol> newRight = new LinkedList<>(first.getRight());
					newRight.remove(delimiter);
					Rule rule = new Rule(first.getLeft(), newRight);
					int index = -1;
					for(Rule r : cfg.getRules()) {
						if(r.equals(rule)) {
							index = cfg.getRules().indexOf(r);
							break;
						}
					}
					for(int i=0;i<cfg.getTerminals().size();i++) {
						ACTION[state.state][i] = "R "+String.valueOf(index);
					}
				}
				continue;
			}
			
			//移入项目
			for(int i=0;i<state.nexts.size();i++) {
				Symbol symbol = state.transfers.get(i);
				Node next = state.nexts.get(i);
				if(TerminalSymbol.class.isInstance(symbol)) {
					//移入项目
					ACTION[state.state][cfg.getTerminals().indexOf(symbol)] = "S "+String.valueOf(next.state);
				}else {
					//待约项目
					GOTO[state.state][cfg.getNonterminals().indexOf(symbol)] = String.valueOf(next.state);
				}
			}
		}
	}
	
	//判断该文法是否是LR0文法，若不是则抛出错误
	private void isLR0() {
		boolean lr0 = true;
		for(Node state : states) {
			if(!check(state)) {
				lr0 = false;
				System.err.println();
				System.err.println(state.state+":");
				System.err.println(state.rules);
			}
		}
		if(!lr0)throw new RuntimeException("该文法不是LR0文法！");
	}



	public static void main(String[] args) {
		// TODO Auto-generated method stub		
//		NontermianlSymbol P = new NontermianlSymbol("Program");
//		NontermianlSymbol B = new NontermianlSymbol("Block");
//		NontermianlSymbol HEAD = new NontermianlSymbol("Block head");
//		NontermianlSymbol TAIL = new NontermianlSymbol("Compound tail");
//		NontermianlSymbol STATEMENT = new NontermianlSymbol("Compound statement");
//		
//		TerminalSymbol begin = new TerminalSymbol("b");
//		TerminalSymbol end = new TerminalSymbol("e");
//		TerminalSymbol d = new TerminalSymbol("d");
//		TerminalSymbol semicolon = new TerminalSymbol(";");
//		TerminalSymbol s = new TerminalSymbol("S");
//
//
//		
//		Rule r1 = new Rule(P, Arrays.asList(B));
//		Rule r2 = new Rule(P, Arrays.asList(STATEMENT));
//		Rule r3 = new Rule(B, Arrays.asList(HEAD,semicolon,TAIL));
//		Rule r4 = new Rule(HEAD, Arrays.asList(begin,d));
//		Rule r5 = new Rule(HEAD, Arrays.asList(HEAD,semicolon,d));
//		Rule r6 = new Rule(TAIL, Arrays.asList(s,end));
//		Rule r7 = new Rule(TAIL, Arrays.asList(s,semicolon,TAIL));
//		Rule r8 = new Rule(STATEMENT, Arrays.asList(begin,TAIL));
//		
//		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5,r6,r7,r8));
//		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(P,B,HEAD,TAIL,STATEMENT));
//		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(begin,end,d,s,semicolon));
//		NontermianlSymbol startSymbol = P;
//		
//		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
//		
//		LR0 lr0 = new LR0(cfg);
		
 		NontermianlSymbol S = new NontermianlSymbol("S");
		NontermianlSymbol A = new NontermianlSymbol("A");
		NontermianlSymbol B = new NontermianlSymbol("B");
		
		TerminalSymbol a = new TerminalSymbol("a");
		TerminalSymbol b = new TerminalSymbol("b");
		TerminalSymbol c = new TerminalSymbol("c");
		TerminalSymbol d = new TerminalSymbol("d");
		TerminalSymbol e = new TerminalSymbol("e");

		
		Rule r1 = new Rule(S, Arrays.asList(a,A,c,B,e));
		Rule r2 = new Rule(A, Arrays.asList(b));
		Rule r3 = new Rule(A, Arrays.asList(A,b));
		Rule r4 = new Rule(B, Arrays.asList(d));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(S,A,B));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(a,b,c,d,e));
		NontermianlSymbol startSymbol = S;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		LR0 lr0 = new LR0(cfg);
		System.out.println(lr0.parse(Arrays.asList(a,b,b,c,d,e)));
	}

}
/*
 
 		NontermianlSymbol S = new NontermianlSymbol("S");
		NontermianlSymbol A = new NontermianlSymbol("A");
		NontermianlSymbol B = new NontermianlSymbol("B");
		
		TerminalSymbol a = new TerminalSymbol("a");
		TerminalSymbol b = new TerminalSymbol("b");
		TerminalSymbol c = new TerminalSymbol("c");
		TerminalSymbol d = new TerminalSymbol("d");
		TerminalSymbol e = new TerminalSymbol("e");

		
		Rule r1 = new Rule(S, Arrays.asList(a,A,c,B,e));
		Rule r2 = new Rule(A, Arrays.asList(b));
		Rule r3 = new Rule(A, Arrays.asList(A,b));
		Rule r4 = new Rule(B, Arrays.asList(d));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(S,A,B));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(a,b,c,d,e));
		NontermianlSymbol startSymbol = S;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		LR0 lr0 = new LR0(cfg);
		lr0.parse(Arrays.asList(a,b,b,c,d,e));
 
*/
