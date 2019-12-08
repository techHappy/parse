package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
public class LR0 extends ButtonUpParsing {

	//分析栈与输入流分隔符
	//在分隔符前的已被识别
	static TerminalSymbol delimiter = new TerminalSymbol('|');
	//用于LR0项目的状态 
	private static int id=-1;
	
	//DFA的状态
	class Node{
		//状态
		int state=id++;
		//̬初始项目
		List<Rule> initialRules;
		//对初始规则闭包运算后的项目
		List<Rule> rules;
		//转向的其他DFA状态和文法符号
		List<Node> nexts;
		List<Symbol> transfers;
		
		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			return initialRules.equals(((Node)obj).initialRules);
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			StringBuilder sb = new StringBuilder();
			sb.append("I"+state+": \n");
			if(rules != null) {
				sb.append("rules: "+rules+"\n");
			}else {
				sb.append("initialRules: "+initialRules+"\n");
			}
			return sb.toString();
		}
	}
	
	//DFA状态集
	protected List<Node> states = new LinkedList<>();
	//LR0分析表
	protected String[][] ACTION,GOTO;
	//拓广文法
	CFG G1;
	//
	Rule startRule;
	
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
						ACTION[state.state][i] = "R"+String.valueOf(index);
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
					ACTION[state.state][cfg.getTerminals().indexOf(symbol)] = "S"+String.valueOf(next.state);
				}else {
					//待约项目
					GOTO[state.state][cfg.getNonterminals().indexOf(symbol)] = String.valueOf(next.state);
				}
			}
		}
	}

	//检查一个DFA的状态的规则集是否有移进-规约冲突和规约-规约冲突
	protected boolean check(Node state) {
		boolean reduce = false;
		for(Rule rule : state.rules) {
			List<Symbol> right = rule.getRight();
			int index = right.indexOf(delimiter);
			
			if(index == right.size()-1) {
				reduce=true;
				break;
			}
		}
		if(reduce && state.rules.size()>1) {
			return false;
		}
		return true;
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
	
	
	//构造DFA
	private void makeDfa() {
		//拓广文法G1
		G1 = cfg;
		NontermianlSymbol S=G1.getStartSymbol();
		//拓广文法的开始符号S1
		NontermianlSymbol S1=new NontermianlSymbol(S.name+"1");
		startRule = new Rule(S1, Arrays.asList(S));
		G1.addRule(startRule);
		G1.setStartSymbol(S1);
		new Node();
		Rule r = new Rule(S1, Arrays.asList(delimiter,S));
		Queue<Node> queue = new LinkedList<>();
		Node start = new Node();
		start.initialRules = Arrays.asList(r);
		start.initialRules.sort(Comparator.comparing(Rule::hashCode));
		queue.add(start);
		states.add(start);
		
		boolean exist = false;
		while(!queue.isEmpty()) {
			Node node = queue.poll();
			//将node.initialRules进行闭包运算得到node.rules
			node.rules = closure(node.initialRules);
			
			Map<Symbol, List<Rule>> map = stackTopSymbol2Rule(node.rules);
			
			node.transfers = new ArrayList<>(map.keySet());
			node.nexts = new ArrayList<>();
			//
			for(Symbol symbol:node.transfers) {
				//移入，若已经构造则直接指向该DFA状态
				List<Rule> inits = moveIn(map.get(symbol));
				inits.sort(Comparator.comparing(Rule::hashCode));
				for(Node s : states) {
					if(inits.equals(s.initialRules)) {
						node.nexts.add(s);
						exist = true;
						break;
					}
				}
				if(!exist) {
					//构造新的DFA状态
					Node n = new Node();
					n.initialRules = inits;
					node.nexts.add(n);
					queue.add(n);
					states.add(n);
				}else {
					exist = false;
				}
				
			}
		}
	}
	
	//把一个项目的分隔符向后移一位
	private Rule moveIn(Rule old) {
		NontermianlSymbol left = old.getLeft();
		List<Symbol> right = old.getRight();
		
		int index = right.indexOf(delimiter);
		List<Symbol> newRight = new ArrayList<>(right);
		newRight.remove(index);
		newRight.add(index+1, delimiter);
		return new Rule(left, newRight);
	}
	
	//把多个项目的分隔符向后移一位
	private List<Rule> moveIn(List<Rule> olds){
		List<Rule> list = new ArrayList<>();
		for(Rule rule : olds) {
			list.add(moveIn(rule));
		}
		return list;
	}
	
	//待分析的符号对项目的映射
	private Map<Symbol, List<Rule>> stackTopSymbol2Rule(List<Rule> rules){
		Map<Symbol, List<Rule>> map = new HashMap<>();
		for(Rule rule : rules) {
			List<Symbol> right = rule.getRight();
			if(right.indexOf(delimiter)+1 >= right.size())continue;
			Symbol symbol = right.get(right.indexOf(delimiter)+1);
			if(map.containsKey(symbol)) {
				map.get(symbol).add(rule);
			}else {
				List<Rule> list = new ArrayList<>();
				list.add(rule);
				map.put(symbol, list);
			}
		}
		return map;
	}
	
	//用于closure中的marked
	private int getIndex(Symbol symbol) {
		if(NontermianlSymbol.class.isInstance(symbol)) {
			return G1.getNonterminals().indexOf(symbol);
		}else {
			return G1.getTerminals().indexOf(symbol)+G1.getNonterminals().size();
		}
	}
	
	private List<Rule> closure(List<Rule> _rules){
		Set<Rule> clo = new HashSet<>();
		for(Rule r:_rules) {
			clo.addAll(closure(r));
		}
		return new ArrayList<>(clo);
	}
	
	//对初始项目进行闭包运算
	private List<Rule> closure(Rule _rule){
		Map<NontermianlSymbol, List<Rule>> map = G1.ruleGroupByNonterminal();
		boolean[] marked = new boolean[cfg.getNonterminals().size()];
		Queue<Rule> queue = new LinkedList<>();
		Set<Rule> clo = new HashSet<>();
		clo.add(_rule);
		queue.add(_rule);
		
		while(!queue.isEmpty()) {
			Rule rule = queue.poll();
			List<Symbol> right = rule.getRight();
		
			
			int i=right.indexOf(delimiter);
			//没有分隔符，抛出错误
			if(i<0)throw new RuntimeException();
			//已经移到最后，跳过
			if(i>=right.size()-1)continue;
			
			Symbol next = right.get(i+1);
			if(TerminalSymbol.class.isInstance(next)) {
				//终结符跳过
				continue;
			}else {
				//非终结符，拓展
				if(marked[getIndex(next)])continue;
				marked[getIndex(next)]=true;
				List<Rule> rules = map.get(next);
				for(Rule r : rules) {
					List<Symbol> newRight = new LinkedList<Symbol>(r.getRight());
					newRight.add(0, delimiter);
					Rule newRule = new Rule(r.getLeft(), newRight);
					clo.add(newRule);
					queue.offer(newRule);
				}
			}
		}

		return new ArrayList<>(clo);
	}

	@Override
	public boolean parse(List<Symbol> symbols) {
		// TODO Auto-generated method stub
		//状态栈
		LinkedList<Integer> stateStack = new LinkedList<>();
		//符号栈
		LinkedList<Symbol> symbolStack = new LinkedList<>();
		//输入流
		List<Symbol> stream = new LinkedList<>(symbols);
		
		stateStack.add(0);
		symbolStack.add(CFG.over);
		stream.add(CFG.over);
		
		do {
			Symbol transfer = stream.get(0);
			String act = ACTION[stateStack.peekLast()][cfg.getTerminals().indexOf(transfer)];
			
			String[] parts = act.split("");
			switch(parts[0]) {
			case "a":
				//接受
				return true;
			case "S":
				//移入
				stream.remove(0);
				symbolStack.add(transfer);
				stateStack.add(Integer.parseInt(parts[1]));
				break;
			case "R":
				//规约
				int index = Integer.parseInt(parts[1]);
				Rule rule = cfg.getRules().get(index);
				//将状态栈和符号栈的指针减去rule.getRight().size()
				for(int i=0;i<rule.getRight().size();i++) {
					stateStack.pollLast();
					symbolStack.pollLast();
				}
				//符号栈加入左边符号
				symbolStack.addLast(rule.getLeft());
				int nextState = Integer.parseInt(GOTO[stateStack.peekLast()][cfg.getNonterminals().indexOf(rule.getLeft())]);
				//状态栈转移
				stateStack.addLast(nextState);
				break;
			}
			
		}while(true);
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub		
		NontermianlSymbol P = new NontermianlSymbol("Program");
		NontermianlSymbol B = new NontermianlSymbol("Block");
		NontermianlSymbol HEAD = new NontermianlSymbol("Block head");
		NontermianlSymbol TAIL = new NontermianlSymbol("Compound tail");
		NontermianlSymbol STATEMENT = new NontermianlSymbol("Compound statement");
		
		TerminalSymbol begin = new TerminalSymbol('b');
		TerminalSymbol end = new TerminalSymbol('e');
		TerminalSymbol d = new TerminalSymbol('d');
		TerminalSymbol semicolon = new TerminalSymbol(';');
		TerminalSymbol s = new TerminalSymbol('S');


		
		Rule r1 = new Rule(P, Arrays.asList(B));
		Rule r2 = new Rule(P, Arrays.asList(STATEMENT));
		Rule r3 = new Rule(B, Arrays.asList(HEAD,semicolon,TAIL));
		Rule r4 = new Rule(HEAD, Arrays.asList(begin,d));
		Rule r5 = new Rule(HEAD, Arrays.asList(HEAD,semicolon,d));
		Rule r6 = new Rule(TAIL, Arrays.asList(s,end));
		Rule r7 = new Rule(TAIL, Arrays.asList(s,semicolon,TAIL));
		Rule r8 = new Rule(STATEMENT, Arrays.asList(begin,TAIL));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5,r6,r7,r8));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(P,B,HEAD,TAIL,STATEMENT));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(begin,end,d,s,semicolon));
		NontermianlSymbol startSymbol = P;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		LR0 lr0 = new LR0(cfg);
	}

}
/*
 
 		NontermianlSymbol S = new NontermianlSymbol("S");
		NontermianlSymbol A = new NontermianlSymbol("A");
		NontermianlSymbol B = new NontermianlSymbol("B");
		
		TerminalSymbol a = new TerminalSymbol('a');
		TerminalSymbol b = new TerminalSymbol('b');
		TerminalSymbol c = new TerminalSymbol('c');
		TerminalSymbol d = new TerminalSymbol('d');
		TerminalSymbol e = new TerminalSymbol('e');

		
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
