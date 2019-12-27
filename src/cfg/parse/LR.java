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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cfg.CFG;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;
import util.Displays;

public abstract class LR extends ButtonUpParsing {
	
	//分析栈与输入流分隔符
	//在分隔符前的已被识别
	static TerminalSymbol delimiter = new TerminalSymbol("|");

	
	//DFA的状态
	class Node{
		//状态
		int state=id++;
		//̬初始项目
		public List<Rule> initialRules;
		
		//对初始规则闭包运算后的项目
		public List<Rule> rules;
		
		//转向的其他DFA状态和文法符号
		public List<Node> nexts;
		public List<Symbol> transfers;
		
		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			return initialRules.equals(((Node)obj).initialRules);
		}
		
		@Override
		public int hashCode() {
			// TODO Auto-generated method stub
			return initialRules.hashCode();
		}
		
		public boolean equalsInHeart(Node node) {
			List<Rule> ruleList1 = initialRules
					.stream()
					.map(r->new Rule(r.getLeft(), r.getRight()))
					.collect(Collectors.toList());
			
			List<Rule> ruleList2 = node.initialRules
					.stream()
					.map(r->new Rule(r.getLeft(), r.getRight()))
					.collect(Collectors.toList());
			return ruleList1.equals(ruleList2);
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
	
	//用于LR0项目的状态 
	private static int id=0;
	
	public static void reallocateId(List<Node> states) {
		id=0;
		states.stream().forEach(s->s.state=id++);
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
		G1 = new CFG(cfg);
		NontermianlSymbol S=G1.getStartSymbol();
		//拓广文法的开始符号S1
		NontermianlSymbol S1=new NontermianlSymbol(S.name+"1");
		//假设文法中不存在非终结符S1
		assert(!G1.getNonterminals().contains(S1));
		//开始产生式
		startRule = new Rule(S1, Arrays.asList(S));
		G1.addRule(startRule);
		//
		G1.setStartSymbol(S1);
		//
		Rule r = new Rule(S1, Arrays.asList(delimiter,S));
		Queue<Node> queue = new LinkedList<>();
		//DFA开始状态
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
			
			Map<Symbol, List<Rule>> map = analyzingSymbol2Rule(node.rules);
			
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
	
	/**
	 * 把一个项目的分隔符向后移一位
	 * 
	 * <p>
	 * 假设分割符不在最后，即项目不是规约项目
	 * 
	 * @param old
	 * @return
	 */
	protected Rule moveIn(Rule old) {
		NontermianlSymbol left = old.getLeft();
		List<Symbol> right = old.getRight();
		
		int index = right.indexOf(delimiter);
		assert(index != right.size()-1);
		
		List<Symbol> newRight = new ArrayList<>(right);
		newRight.remove(index);
		newRight.add(index+1, delimiter);
		return new Rule(left, newRight);
	}
	
	/**
	 * 把多个项目的分隔符向后移一位
	 * @param olds
	 * @return
	 */
	protected List<Rule> moveIn(List<Rule> olds){
		List<Rule> list = new ArrayList<>();
		for(Rule rule : olds) {
			list.add(moveIn(rule));
		}
		return list;
	}
	
	/**
	 * 待分析的符号对项目集的映射
	 * @param rules 项目集的所有项目
	 * @return 待分析的符号对项目集的映射
	 */
	protected Map<Symbol, List<Rule>> analyzingSymbol2Rule(List<Rule> rules){
		Map<Symbol, List<Rule>> map = new HashMap<>();
		for(Rule rule : rules) {
			List<Symbol> right = rule.getRight();
			if(right.indexOf(delimiter)+1 >= right.size())continue;
			
			Symbol symbol = right.get(right.indexOf(delimiter)+1);
			map.computeIfAbsent(symbol, (k) -> new ArrayList<>()).add(rule);
		}
		return map;
	}
	
	//用于closure中的marked
	protected int getIndex(Symbol symbol) {
		if(NontermianlSymbol.class.isInstance(symbol)) {
			return G1.getNonterminals().indexOf(symbol);
		}else {
			return G1.getTerminals().indexOf(symbol)+G1.getNonterminals().size();
		}
	}
	
	protected List<Rule> closure(List<Rule> _rules){
		Set<Rule> clo = new HashSet<>();
		for(Rule r:_rules) {
			clo.addAll(closure(r));
		}
		return new ArrayList<>(clo);
	}
	
	//对初始项目进行闭包运算
	protected List<Rule> closure(Rule _rule){
		Map<NontermianlSymbol, List<Rule>> map = G1.ruleGroupByNonterminal();
		boolean[] marked = new boolean[cfg.getNonterminals().size()];
		Queue<Rule> queue = new LinkedList<>();
		//闭包
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
	
	/**
	 * 检查一个DFA的状态的规则集是否有移进-规约冲突和规约-规约冲突
	 * @param state 一个DFA的状态
	 * @return
	 */
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
	
	protected void displayTable() {
		Displays.displayTable(
				ACTION, 
				IntStream.range(0, states.size()).collect(ArrayList::new, ArrayList::add, ArrayList::addAll), 
				cfg.getTerminals(),
				"ACTION");
		Displays.displayTable(
				GOTO, 
				IntStream.range(0, states.size()).collect(ArrayList::new, ArrayList::add, ArrayList::addAll), 
				cfg.getNonterminals(),
				"GOTO");
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
			
			String[] parts = act.split("\\s+");
			switch(parts[0]) {
			case "acc":
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
				//状态栈转移
				int nextState = Integer.parseInt(GOTO[stateStack.peekLast()][cfg.getNonterminals().indexOf(rule.getLeft())]);
				stateStack.addLast(nextState);
				break;
			}
			
		}while(true);
		
	}
}
