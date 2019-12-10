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
import cfg.parse.LR.Node;

public abstract class LR extends ButtonUpParsing {
	
	//分析栈与输入流分隔符
	//在分隔符前的已被识别
	static TerminalSymbol delimiter = new TerminalSymbol("|");
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
	
	

	public LR(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
	}

	//DFA状态集
	protected List<Node> states = new LinkedList<>();
	//LR0分析表
	protected String[][] ACTION,GOTO;
	//拓广文法
	CFG G1;
	//
	Rule startRule;

	//构造DFA
	protected void makeDfa() {
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
}
