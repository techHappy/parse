package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
			cfg.removeLeftRecursion();
			if(cfg.isLL1()) makePredicate();
			else System.err.println("该文法不是LL1文法，不能采用预测分析法！");
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
		
		TerminalSymbol plus = new TerminalSymbol('+');
		TerminalSymbol times = new TerminalSymbol('*');
		TerminalSymbol lp = new TerminalSymbol('(');
		TerminalSymbol rp = new TerminalSymbol(')');
		TerminalSymbol i = new TerminalSymbol('i');
		
		Rule r1 = new Rule(E, Arrays.asList(E,plus,T));
		Rule r2 = new Rule(E, Arrays.asList(T));
		Rule r3 = new Rule(T, Arrays.asList(T,times,F));
		Rule r4 = new Rule(T, Arrays.asList(F));
		Rule r5 = new Rule(F, Arrays.asList(i));
		Rule r6 = new Rule(F, Arrays.asList(lp,E,rp));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5,r6));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(E,T,F));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(plus,times,lp,rp,i));
		NontermianlSymbol startSymbol = E;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		System.out.println(cfg.isLL1());
		cfg.removeLeftRecursion();
		System.out.println(cfg.isLL1());
 
		for(Rule rule : cfg.getRules()) {
			System.out.printf("%-20s",rule);
			System.out.println(cfg.SELECT(rule));
		}
		
		Predicate predicate = new Predicate(cfg);
		predicate.parse(Arrays.asList(i,plus,i,times,i));
	}
}
