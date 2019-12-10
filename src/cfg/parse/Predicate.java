package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import cfg.CFG;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;

public class Predicate extends TopDownParsing {

	public static final List<Symbol> ERROR = null;
	
	List<Symbol>[][] predicate;
	
	@SuppressWarnings("unchecked")
	public Predicate(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
		predicate = new List[cfg.getNonterminals().size()][cfg.getTerminals().size()];
		if(cfg.isLL1())	makePredicate();
		else {
			removeLeftRecursion();
			if(cfg.isLL1()) makePredicate();
			else System.err.println("���ķ�����LL1�ķ������ܲ���Ԥ���������");
		}
	}
	

	

	private void makePredicate() {
		for(Rule rule : cfg.getRules()) {
			NontermianlSymbol nontermianlSymbol = rule.getLeft();
			for(TerminalSymbol terminalSymbol : cfg.SELECT(rule)) {
				predicate[getIndex(nontermianlSymbol)][getIndex(terminalSymbol)] = rule.getRight();
			}
		}
	}
	
	private int getIndex(Symbol symbol) {		
		if(NontermianlSymbol.class.isInstance(symbol)) {
			return cfg.getNonterminals().indexOf(symbol);
		}else {
			return cfg.getTerminals().indexOf(symbol);
		}
	}
	
	public void removeLeftRecursion() {
		for(int i=0;i<cfg.getNonterminals().size();i++) {
			Map<NontermianlSymbol, List<Rule>> map = cfg.ruleGroupByNonterminal();
			List<Rule> iRules = map.get(cfg.getNonterminals().get(i));
			for(int j=0;j<i;j++) {
				List<Rule> jRules = map.get(cfg.getNonterminals().get(j));
				for(int k=0;k<iRules.size();k++) {
					Rule rule = iRules.get(k);
					if(rule.getRight().get(0) == cfg.getNonterminals().get(j)) {
						iRules.remove(rule);
						cfg.getRules().remove(rule);
						//
						k--;
						
						for(int l=0;l<jRules.size();l++) {
							List<Symbol> right = new ArrayList<>();
							right.addAll(jRules.get(l).getRight());
							right.addAll(rule.getRight().subList(1, rule.getRight().size()));
							
							Rule r = new Rule(cfg.getNonterminals().get(i), right);
							
							cfg.getRules().add(r);
							iRules.add(r);
						}
					}
				}
			}
			
			if(hasDirectLeftRecursion(iRules)) {
				NontermianlSymbol A1 = new NontermianlSymbol(cfg.getNonterminals().get(i).name+"1");
				cfg.getNonterminals().add(A1);
				for(Rule rule : iRules) {
					cfg.getRules().remove(rule);
					List<Symbol> right = new ArrayList<>();
					if(rule.getRight().get(0) == rule.getLeft()) {
						right.addAll(rule.getRight().subList(1, rule.getRight().size()));
						right.add(A1);
						cfg.getRules().add(new Rule(A1, right));
					}else {
						right.addAll(rule.getRight());
						right.add(A1);
						cfg.getRules().add(new Rule(cfg.getNonterminals().get(i), right));
					}
				}
				
				cfg.getRules().add(new Rule(A1, Arrays.asList(CFG.epsilon)));
			}
		}

		Map<NontermianlSymbol, List<Rule>> map = cfg.ruleGroupByNonterminal();
		Queue<NontermianlSymbol> queue = new LinkedList<>();
		Set<NontermianlSymbol> reserved = new HashSet<>();
		reserved.add(cfg.getStartSymbol());
		
		boolean[] marked = new boolean[cfg.getNonterminals().size()];
		marked[cfg.getNonterminals().indexOf(cfg.getStartSymbol())] = true;
		queue.add(cfg.getStartSymbol());
		
		while(!queue.isEmpty()) {
			NontermianlSymbol nontermianlSymbol = queue.remove();
			for(Rule rule : map.get(nontermianlSymbol)) {
				List<Symbol> right = rule.getRight();
				for(Symbol symbol : right) {
					if(NontermianlSymbol.class.isInstance(symbol) 
							&& symbol != rule.getLeft()
							&& !marked[cfg.getNonterminals().indexOf(symbol)]) {
						marked[cfg.getNonterminals().indexOf(symbol)] = true;
						queue.add((NontermianlSymbol) symbol);
						reserved.add((NontermianlSymbol) symbol);
					}
				}
			}
		}
		
		Iterator<Rule> it = cfg.getRules().iterator();
		while(it.hasNext()) {
			Rule rule = it.next();
			if(!reserved.contains(rule.getLeft())) {
				it.remove();
			}
		}
		cfg.getTerminals().add(CFG.epsilon);
	}

	private boolean hasDirectLeftRecursion(List<Rule> rules) {
		for(Rule rule : rules) {
			if(rule.getLeft() == rule.getRight().get(0)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean parse(List<Symbol> symbols) {
		// TODO Auto-generated method stub
		Stack<Symbol> stack = new Stack<>();
		List<Symbol> stream = new LinkedList<Symbol>(symbols);
		stack.add(CFG.over);
		stack.add(cfg.getStartSymbol());
		stream.add(CFG.over);
		do {
			Symbol top=stack.pop();
			if(NontermianlSymbol.class.isInstance(top)) {
				List<Symbol> list = new ArrayList<Symbol>(predicate[getIndex(top)][getIndex(stream.get(0))]);
				if(list.size() == 1 && list.get(0) == CFG.epsilon)continue;
				int n=list.size();
				for(int i=n-1;i>=0;i--) {
					stack.add(list.remove(i));
				}
			}else {
				if(top==stream.get(0))stream.remove(0);
				else return false;
			}
		}while(!(stack.peek()==CFG.over && stream.get(0)==CFG.over));
		return true;
	}

	public static void main(String[] args) {
		NontermianlSymbol E = new NontermianlSymbol("E");
		NontermianlSymbol T = new NontermianlSymbol("T");
		NontermianlSymbol F = new NontermianlSymbol("F");
		
		TerminalSymbol plus = new TerminalSymbol("+");
		TerminalSymbol times = new TerminalSymbol("*");
		TerminalSymbol lp = new TerminalSymbol("(");
		TerminalSymbol rp = new TerminalSymbol(")");
		TerminalSymbol i = new TerminalSymbol("i");
		
		Rule r1 = new Rule(E, Arrays.asList(E,plus,T));
		Rule r2 = new Rule(E, Arrays.asList(T));
		Rule r3 = new Rule(T, Arrays.asList(T,times,F));
		Rule r4 = new Rule(T, Arrays.asList(F));
		Rule r5 = new Rule(F, Arrays.asList(i));
		Rule r6 = new Rule(F, Arrays.asList(lp,E,rp));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5,r6));
		Set<NontermianlSymbol> nontermianlSymbols = new HashSet<>(Arrays.asList(E,T,F));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(plus,times,lp,rp,i));
		NontermianlSymbol startSymbol = E;
		
		CFG cfg = new CFG(rules, nontermianlSymbols,terminals,startSymbol);
 
		for(Rule rule : cfg.getRules()) {
			System.out.printf("%-20s",rule);
			System.out.println(cfg.SELECT(rule));
		}
		
		Predicate predicate = new Predicate(cfg);
		predicate.parse(Arrays.asList(i,plus,i,times,i));
	}
}
