package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import cfg.CFG;
import cfg.LR1Rule;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;
import cfg.parse.LR.Node;

public class LR1 extends LR {

	public LR1(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
		makeDfa();
		makeLR1Table();
	}
	
	
	
	//构造DFA
	@Override
	protected void makeDfa() {
		//拓广文法G1
		G1 = cfg;
		NontermianlSymbol S=G1.getStartSymbol();
		//拓广文法的开始符号S1
		NontermianlSymbol S1=new NontermianlSymbol(S.name+"1");
		startRule = new Rule(S1, Arrays.asList(S));
		G1.addRule(startRule);
		G1.setStartSymbol(S1);
		//DFA初始状态的初始规则
		LR1Rule r = new LR1Rule(S1, Arrays.asList(delimiter,S),new HashSet<>(Arrays.asList(CFG.over)));
		//辅助队列
		Queue<Node> queue = new LinkedList<>();
		//DFA初始状态
		Node start = new Node();
		start.initialRules = Arrays.asList(r);
		//在序列比较相等时，如果每个序列按照确定的规则排序，就可以直接通过equals比较
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

	
	@Override
	protected List<Rule> closure(List<Rule> _rules) {
		// TODO Auto-generated method stub
		Set<Rule> clo = new HashSet<>();
		for(Rule r:_rules) {
			clo.addAll(closure(r));
		}
		return new ArrayList<>(clo);
	}
	
	@Override
	protected List<Rule> closure(Rule _rule) {
		// TODO Auto-generated method stub
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
				LR1Rule lr1Rule = (LR1Rule)rule;
				//LR1项目构造前向搜索符集
				Symbol[] symbols=null;
				Set<TerminalSymbol> forwards=null;
				if(right.size() <= i+2) {
					//beta == epsilon
					symbols = lr1Rule.getForwards().toArray(new TerminalSymbol[] {});
					forwards = new HashSet<TerminalSymbol>();
					Collections.addAll(forwards, (TerminalSymbol[])symbols);
				}else {
					//beta != epsilon
					List<Symbol> list = new ArrayList<>(Arrays.asList(right.get(i+2)));
					Collections.addAll(list, lr1Rule.getForwards().toArray(new Symbol[] {}));
					symbols=list.toArray(new Symbol[] {});
					forwards = cfg.FIRST(symbols);
				}
				//前向搜索符集
				for(Rule r : rules) {
					List<Symbol> newRight = new LinkedList<Symbol>(r.getRight());
					newRight.add(0, delimiter);
					LR1Rule newRule = new LR1Rule(r.getLeft(), newRight,forwards);
					
					clo.add(newRule);
					queue.offer(newRule);
				}
			}
		}

		return new ArrayList<>(clo);
	}
	
	//把一个项目的分隔符向后移一位
	@Override
	protected Rule moveIn(Rule old) {
		NontermianlSymbol left = old.getLeft();
		List<Symbol> right = old.getRight();
		
		int index = right.indexOf(delimiter);
		List<Symbol> newRight = new ArrayList<>(right);
		newRight.remove(index);
		newRight.add(index+1, delimiter);
		//LR1中加入
		return new LR1Rule(left, newRight,((LR1Rule)old).getForwards());
	}
	
	protected void makeLR1Table() {
		ACTION = new String[states.size()][cfg.getTerminals().size()];
		GOTO = new String[states.size()][cfg.getNonterminals().size()];
		
		//根据起始项目构造接受项目
		List<Symbol> rList = new ArrayList<>(startRule.getRight());
		rList.add(delimiter);
		Rule accRule = new Rule(startRule.getLeft(), rList);
				
		for(Node state : states) {
			for(Rule rule : state.rules) {
				if(rule.getRight().lastIndexOf(delimiter) == rule.getRight().size()-1) {
					if(rule.equals(accRule)) {
						//接受项目
						ACTION[state.state][cfg.getTerminals().indexOf(CFG.over)] = "acc";
					}else {
						//规约项目
						//Ri，i表示根据第几条规则规约，在这由CFG的规则顺序决定
						List<Symbol> newRight = new LinkedList<>(rule.getRight());
						newRight.remove(delimiter);
						Rule rule1 = new Rule(rule.getLeft(), newRight);
						int index = -1;
						for(Rule r : cfg.getRules()) {
							if(r.equals(rule1)) {
								index = cfg.getRules().indexOf(r);
								break;
							}
						}
						//根据前向搜索集，来构造分析表r
						Set<TerminalSymbol> foward = ((LR1Rule)rule).getForwards();
						for(TerminalSymbol terminalSymbol : foward) {
							ACTION[state.state][cfg.getTerminals().indexOf(terminalSymbol)] = "R "+String.valueOf(index);
						}
					}
				}else {
					//移入项目
					Symbol symbol = rule.getRight().get(rule.getRight().indexOf(delimiter)+1);
					Node next = state.nexts.get(state.transfers.indexOf(symbol));
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
	}
	
	@Override
	protected List<Rule> moveIn(List<Rule> olds) {
		// TODO Auto-generated method stub
		List<Rule> list = new ArrayList<>();
		for(Rule rule : olds) {
			list.add(moveIn(rule));
		}
		return list;
	}
	
	
	public static void main(String[] args) {
//		LR1 lr1 = new LR1(CFG.makeCFG(
//				"S -> a A d \n"
//				+ "S -> b A c \n"
//				+ "S -> a e c\n"
//				+ "S -> b e d\n"
//				+ "A -> e", "S"));
		
		CFG cfg = CFG.makeCFG(
				"S -> B B \n"
				+ "B -> a B \n"
				+ "B -> b\n", "S");
		TerminalSymbol a = new TerminalSymbol("a");
		TerminalSymbol b = new TerminalSymbol("b");
		new LR1(cfg).parse(Arrays.asList(a,b));
	}

}
