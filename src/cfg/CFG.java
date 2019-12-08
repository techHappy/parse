package cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CFG {
	//一锟斤拷CFG锟侥癸拷锟津集猴拷
	//为锟斤拷锟姐法锟斤拷实锟街讹拷为锟斤拷锟斤拷锟絃ist
	private List<Rule> rules;
	
	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	//一锟斤拷CFG锟侥凤拷锟秸斤拷锟斤拷锟斤拷锟�
	//为锟斤拷锟姐法锟斤拷实锟街讹拷为锟斤拷锟斤拷锟絃ist
	private final List<NontermianlSymbol> nonterminals;
	//一锟斤拷CFG锟斤拷锟秸斤拷锟斤拷锟�
	private final Set<TerminalSymbol> terminals;
	//一锟斤拷CFG锟侥匡拷始锟斤拷锟秸斤拷锟�
	private NontermianlSymbol startSymbol;
	
	public void setStartSymbol(NontermianlSymbol startSymbol) {
		this.startSymbol = startSymbol;
	}

	public List<NontermianlSymbol> getNonterminals() {
		return nonterminals;
	}

	public List<Rule> getRules() {
		return rules;
	}

	public List<TerminalSymbol> getTerminals() {
		return new ArrayList<>(terminals);
	}

	public NontermianlSymbol getStartSymbol() {
		return startSymbol;
	}

	private static final int UNDETERMINED = 0;
	private static final int TRUE = 1;
	private static final int FALSE = 2;
	//
	public static final TerminalSymbol epsilon = new TerminalSymbol('蔚');
	
	public static final TerminalSymbol over = new TerminalSymbol('#');
	
	private int[] nonCanInferEpsilon;
	
	//每锟斤拷锟斤拷锟秸斤拷锟斤拷锟紽IRST锟斤拷
	private Set<TerminalSymbol>[] FIRST;  
	//每锟斤拷锟斤拷锟秸斤拷锟斤拷锟紽OLLOW锟斤拷
	private Set<TerminalSymbol>[] FOLLOW;
	//每锟斤拷锟斤拷锟斤拷锟絊ELECT锟斤拷
	private Set<TerminalSymbol>[] SELECT;
	
	private boolean isLL1;
	
	//
	
	public CFG(Set<Rule> rules, Set<NontermianlSymbol> nonterminals, Set<TerminalSymbol> terminals,
			NontermianlSymbol startSymbol) {
		super();
		this.rules = new ArrayList<>(rules);
		this.nonterminals = new ArrayList<>(nonterminals);
		this.terminals = new HashSet<>(terminals);
		this.startSymbol = startSymbol;


		
		caculateIsLL1();
	}
	
	public CFG(CFG cfg) {
		super();
		this.rules = new ArrayList<>(cfg.rules);
		this.nonterminals = new ArrayList<>(cfg.nonterminals);
		this.terminals = new HashSet<>(cfg.terminals);
		this.startSymbol = cfg.startSymbol;
		this.isLL1 = cfg.isLL1;

	}
	
	private void calculateFIRST() {
		for(int i=0;i<FIRST.length;i++) {
			FIRST[i] = new HashSet<>(); 
		}
		
		@SuppressWarnings("unchecked")
		Set<TerminalSymbol>[] oldFIRST = new HashSet[FIRST.length];
		do {
			for(int i=0;i<FIRST.length;i++) {
				oldFIRST[i] = new HashSet<>(FIRST[i]);
			}
			
			for(Rule rule : rules) {
				if(rule.right.size() == 1 && rule.right.get(0) == epsilon) {
					FIRST[nonterminals.indexOf(rule.left)].add(epsilon);
				}else if(terminals.contains(rule.right.get(0))) {
					FIRST[nonterminals.indexOf(rule.left)].add((TerminalSymbol) rule.right.get(0));
				}else {
					int i=0;
					for(;i<rule.right.size();i++) {
						Symbol symbol = rule.right.get(i);
						if(NontermianlSymbol.class.isInstance(symbol) && 
							nonCanInferEpsilon[nonterminals.indexOf(symbol)] == TRUE){
							Set<TerminalSymbol> t = new HashSet<>(FIRST[nonterminals.indexOf(symbol)]);
							t.remove(epsilon);
							FIRST[nonterminals.indexOf(rule.left)].addAll(t);	
						}else if(NontermianlSymbol.class.isInstance(symbol)){
							Set<TerminalSymbol> t = new HashSet<>(FIRST[nonterminals.indexOf(symbol)]);
							t.remove(epsilon);
							FIRST[nonterminals.indexOf(rule.left)].addAll(t);
							break;
						}else {
							break;
						}
					}
					if(i == rule.right.size()) {
						FIRST[nonterminals.indexOf(rule.left)].add(epsilon);
					}
				}
				
			}
		}while(isChanged(oldFIRST, FIRST));

	}
	
	private void caculateFLLOW() {
		for(int i=0;i<FOLLOW.length;i++) {
			FOLLOW[i] = new HashSet<>(); 
		}
		FOLLOW[nonterminals.indexOf(startSymbol)].add(over);
		terminals.add(over);
		
		@SuppressWarnings("unchecked")
		Set<TerminalSymbol>[] oldFOLLOW = new HashSet[FOLLOW.length];
		do {
			for(int i=0;i<FOLLOW.length;i++) {
				oldFOLLOW[i] = new HashSet<>(FOLLOW[i]);
			}
			
			for(Rule rule : rules) {
				int i = 0;
				List<Symbol> right = rule.right;
				for(;i<right.size();i++) {
					Symbol symbol = right.get(i);
					if(NontermianlSymbol.class.isInstance(symbol)) {
						if(i == right.size()-1) {
							FOLLOW[nonterminals.indexOf(symbol)].addAll(
									FOLLOW[nonterminals.indexOf(rule.left)]);
						}else {
							Set<TerminalSymbol> set = FIRST(right.subList(i+1, right.size()).toArray(new Symbol[right.size()-i-1]));
							
							if(set.contains(epsilon)) {
								FOLLOW[nonterminals.indexOf(symbol)].addAll(
										FOLLOW[nonterminals.indexOf(rule.left)]);
							}
							
							set.remove(epsilon);
							FOLLOW[nonterminals.indexOf(symbol)].addAll(set);
						}
					}
				}
				
			}
		}while(isChanged(oldFOLLOW, FOLLOW));
	}
	
	private void caculateSELECT() {
		for(Rule rule : rules) {
			Set<TerminalSymbol> set = new HashSet<>();
			
			Set<TerminalSymbol> first = FIRST(rule.right.toArray(new Symbol[rule.right.size()]));
			if(first.contains(epsilon)) {
				first.remove(epsilon);
				set.addAll(first);
				set.addAll(FOLLOW(rule.left));
			}else {
				set.addAll(first);
			}
			
			SELECT[rules.indexOf(rule)] = set;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void caculateIsLL1() {
		nonCanInferEpsilon = getNonterminalOfInferringEpsilon();
		FIRST = new HashSet[nonterminals.size()];
		FOLLOW = new HashSet[nonterminals.size()];
		SELECT = new HashSet[rules.size()];
		calculateFIRST();
		caculateFLLOW();
		caculateSELECT();
		Map<NontermianlSymbol, List<Rule>> map = ruleGroupByNonterminal();
		
		isLL1 = true;
		for(List<Rule> list : map.values()) {
			for(int i=0;i<list.size();i++) {
				Set<TerminalSymbol> select1 = new HashSet<>(SELECT(list.get(i)));
				List<Symbol> right1 = list.get(i).right;
				Set<TerminalSymbol> first1 = FIRST(right1.toArray(new Symbol[right1.size()]));
				for(int j=i+1;j<list.size();j++) {
					Set<TerminalSymbol> select2 = new HashSet<>(SELECT(list.get(j)));
					List<Symbol> right2 = list.get(i).right;
					Set<TerminalSymbol> first2 = FIRST(right2.toArray(new Symbol[right2.size()]));
					
					if(first1.contains(epsilon) || first2.contains(epsilon)) {
						isLL1 = false;
						return;
					}
					
					Set<TerminalSymbol> set = new HashSet<>(select1);
					set.retainAll(select2);
					if(!set.isEmpty()) {
						isLL1 = false;
						return;
					}
				}
			}
		}
	}

	private boolean isChanged(Set<TerminalSymbol>[] old,Set<TerminalSymbol>[] cur) {
		int oldSize = 0,curSize = 0;
		for(Set<TerminalSymbol> set : old) {
			oldSize += set.size();
		}
		for(Set<TerminalSymbol> set : cur) {
			curSize += set.size();
		}
		return !(oldSize == curSize);
	}
		
	private int[] getNonterminalOfInferringEpsilon() {
		int[] canInferring = new int[nonterminals.size()];
		Arrays.fill(canInferring, UNDETERMINED);
		
		List<Rule> _rules = new LinkedList<Rule>(rules);
		//扫锟斤拷锟侥凤拷锟叫的诧拷锟斤拷式
		{
			Set<NontermianlSymbol> toBeDeletedSet = new HashSet<>();
			Set<NontermianlSymbol> reservedSet = new HashSet<>();
			{
				boolean reserved = true;
				Iterator<Rule> it = _rules.iterator();
				while(it.hasNext()) {
					Rule r = it.next();
					reserved = true;
					
					if(r.right.size() == 1 && r.right.get(0) == epsilon) {
						canInferring[nonterminals.indexOf(r.left)] = TRUE;
						toBeDeletedSet.add(r.left);
						reservedSet.add(r.left);
					}else {
						for(Symbol symbol : r.right) {
							if(TerminalSymbol.class.isInstance(symbol)) {
								it.remove();
								reserved = false;
								break;
							}
						}
						if(reserved) {
							reservedSet.add(r.left);
						}
					}
				}	
			}
			{
				if(nonterminals.size() != reservedSet.size()) {
					Set<NontermianlSymbol> deleted = new HashSet<>(nonterminals);
					deleted.removeAll(reservedSet);
					for(NontermianlSymbol nontermianl : deleted) {
						canInferring[nonterminals.indexOf(nontermianl)] = FALSE;
					}
				}
			}		
			{
				Iterator<Rule> it = _rules.iterator();
				while(it.hasNext()) {
					Rule r = it.next();
					if(toBeDeletedSet.contains(r.left)) {
						it.remove();
					}
				}
			}
			
		}
		//扫锟斤拷锟斤拷锟绞斤拷也锟斤拷锟矫恳伙拷锟斤拷锟�
		while(!_rules.isEmpty()) {
			{
				Set<NontermianlSymbol> toBeDeletedSet = new HashSet<>();
				boolean exist = false;
				
				Iterator<Rule> ruleIterator = _rules.iterator();
				while(ruleIterator.hasNext()) {
					Rule r = ruleIterator.next();
					
					exist = false;
					List<Symbol> right = new LinkedList<>(r.right);
					Iterator<Symbol> symbolIterator = right.iterator();
					while(symbolIterator.hasNext()) {
						//锟斤拷锟斤拷锟斤拷锟秸斤拷锟�
						NontermianlSymbol symbol = (NontermianlSymbol) symbolIterator.next();
						
						if(canInferring[nonterminals.indexOf(symbol)] == TRUE) {
							symbolIterator.remove();
							if(!symbolIterator.hasNext()) {
								canInferring[nonterminals.indexOf(r.left)] = TRUE;
								toBeDeletedSet.add(r.left);
							}
						}else if(canInferring[nonterminals.indexOf(symbol)] == FALSE) {
							ruleIterator.remove();
							for(Rule rule : _rules) {
								if(rule.left == r.left) {
									exist = true;
									break;
								}
							}
							if(!exist) {
								canInferring[nonterminals.indexOf(r.left)] =FALSE;
							}
						}
					}
					
				}
				
				Iterator<Rule> it = _rules.iterator();
				while(it.hasNext()) {
					Rule r = it.next();
					if(toBeDeletedSet.contains(r.left)) {
						it.remove();
					}
				}
			}
		}

		
		return canInferring;
	}
	
	public Set<TerminalSymbol> FIRST(Symbol ... symbols){
		assert(symbols.length > 0);
		Set<TerminalSymbol> set = new HashSet<>();
		
		if(symbols.length == 1) {
			Symbol symbol = symbols[0];
			if(NontermianlSymbol.class.isInstance(symbol)) {
				if(nonterminals.contains(symbol)) {
					set.addAll(FIRST[nonterminals.indexOf(symbol)]);
				}else {
					throw new RuntimeException();
				}
			}else {
				if(terminals.contains(symbol)) {
					set.add((TerminalSymbol) symbol);
				}else {
					if(symbol == epsilon) {
						set.add(epsilon);
					}else {
						throw new RuntimeException();
					}
				}
			}
		}else {
			int i = 0;
			for(;i<symbols.length;i++) {
				Symbol symbol = symbols[i];
				if(NontermianlSymbol.class.isInstance(symbol)) {
					if(nonCanInferEpsilon[nonterminals.indexOf(symbol)] == TRUE) {
						Set<TerminalSymbol> t = new HashSet<>(FIRST[nonterminals.indexOf(symbol)]);
						t.remove(epsilon);
						set.addAll(t);	
					}else {
						Set<TerminalSymbol> t = new HashSet<>(FIRST[nonterminals.indexOf(symbol)]);
						t.remove(epsilon);
						set.addAll(t);	
						break;
					}
				}else {
					set.add((TerminalSymbol) symbol);
					break;
				}
			}
			if(i == symbols.length) {
				set.add(epsilon);
			}
		}
		
		return set;
	}
	
	public Set<TerminalSymbol> FOLLOW(NontermianlSymbol nontermianlSymbol){			
		return FOLLOW[nonterminals.indexOf(nontermianlSymbol)];
	}
	
	public Set<TerminalSymbol> SELECT(Rule rule){
		return SELECT[rules.indexOf(rule)];
	}
	
	public boolean isLL1() {
		caculateIsLL1();
		return isLL1;
	}
	
	/**
	 * 将一个文法中的产生式按照产生式左边的非终结符分类，得到非终结符对产生式列表的映射
	 * @return 返回非终结符对产生式列表的映射
	 */
	public Map<NontermianlSymbol, List<Rule>> ruleGroupByNonterminal(){
		Map<NontermianlSymbol, List<Rule>> map = new HashMap<>();
		for(Rule rule : rules) {
			if(map.containsKey(rule.left)) {
				map.get(rule.left).add(rule);
			}else {
				List<Rule> list = new ArrayList<>();
				list.add(rule);
				map.put(rule.left, list);
			}
		}
		return map;
	}
	
	public void addRule(Rule rule) {
		if(!rules.contains(rule)) {
			rules.add(rule);
			NontermianlSymbol left = rule.left;
			if(!nonterminals.contains(left))nonterminals.add(left);
			for(Symbol symbol:rule.right) {
				if(NontermianlSymbol.class.isInstance(symbol)) {
					if(!nonterminals.contains(symbol))
						nonterminals.add((NontermianlSymbol)symbol);
				}
			}
		}
	}
	
	
	public void removeLeftRecursion() {
		for(int i=0;i<nonterminals.size();i++) {
			Map<NontermianlSymbol, List<Rule>> map = ruleGroupByNonterminal();
			List<Rule> iRules = map.get(nonterminals.get(i));
			for(int j=0;j<i;j++) {
				List<Rule> jRules = map.get(nonterminals.get(j));
				for(int k=0;k<iRules.size();k++) {
					Rule rule = iRules.get(k);
					if(rule.right.get(0) == nonterminals.get(j)) {
						iRules.remove(rule);
						rules.remove(rule);
						//
						k--;
						
						for(int l=0;l<jRules.size();l++) {
							List<Symbol> right = new ArrayList<>();
							right.addAll(jRules.get(l).right);
							right.addAll(rule.right.subList(1, rule.right.size()));
							
							Rule r = new Rule(nonterminals.get(i), right);
							
							rules.add(r);
							iRules.add(r);
						}
					}
				}
			}
			
			if(hasDirectLeftRecursion(iRules)) {
				NontermianlSymbol A1 = new NontermianlSymbol(nonterminals.get(i).name+"1");
				nonterminals.add(A1);
				for(Rule rule : iRules) {
					rules.remove(rule);
					List<Symbol> right = new ArrayList<>();
					if(rule.right.get(0) == rule.left) {
						right.addAll(rule.right.subList(1, rule.right.size()));
						right.add(A1);
						rules.add(new Rule(A1, right));
					}else {
						right.addAll(rule.right);
						right.add(A1);
						rules.add(new Rule(nonterminals.get(i), right));
					}
				}
				
				rules.add(new Rule(A1, Arrays.asList(epsilon)));
			}
		}

		Map<NontermianlSymbol, List<Rule>> map = ruleGroupByNonterminal();
		Queue<NontermianlSymbol> queue = new LinkedList<>();
		Set<NontermianlSymbol> reserved = new HashSet<>();
		reserved.add(startSymbol);
		
		boolean[] marked = new boolean[nonterminals.size()];
		marked[nonterminals.indexOf(startSymbol)] = true;
		queue.add(startSymbol);
		
		while(!queue.isEmpty()) {
			NontermianlSymbol nontermianlSymbol = queue.remove();
			for(Rule rule : map.get(nontermianlSymbol)) {
				List<Symbol> right = rule.right;
				for(Symbol symbol : right) {
					if(NontermianlSymbol.class.isInstance(symbol) 
							&& symbol != rule.left
							&& !marked[nonterminals.indexOf(symbol)]) {
						marked[nonterminals.indexOf(symbol)] = true;
						queue.add((NontermianlSymbol) symbol);
						reserved.add((NontermianlSymbol) symbol);
					}
				}
			}
		}
		
		Iterator<Rule> it = rules.iterator();
		while(it.hasNext()) {
			Rule rule = it.next();
			if(!reserved.contains(rule.left)) {
				it.remove();
			}
		}
		terminals.add(epsilon);
	}

	private boolean hasDirectLeftRecursion(List<Rule> rules) {
		for(Rule rule : rules) {
			if(rule.left == rule.right.get(0)) {
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		NontermianlSymbol procedure = new NontermianlSymbol("procedure");
//		NontermianlSymbol subprocedure = new NontermianlSymbol("subprocedure");
//		NontermianlSymbol constDefinition = new NontermianlSymbol("constDefinition");
//		NontermianlSymbol varDefinition = new NontermianlSymbol("varDefinition");
//		NontermianlSymbol statement = new NontermianlSymbol("statement");
//		NontermianlSymbol condition = new NontermianlSymbol("condition");
//		NontermianlSymbol expression = new NontermianlSymbol("expression");
//		NontermianlSymbol term = new NontermianlSymbol("term");
//		NontermianlSymbol factor = new NontermianlSymbol("factor");
		
		
		NontermianlSymbol S = new NontermianlSymbol("S");
		NontermianlSymbol T = new NontermianlSymbol("T");
		
		TerminalSymbol a = new TerminalSymbol('a');
		TerminalSymbol and = new TerminalSymbol('^');
		TerminalSymbol lp = new TerminalSymbol('(');
		TerminalSymbol rp = new TerminalSymbol(')');
		TerminalSymbol comma = new TerminalSymbol(',');
		
		Rule r1 = new Rule(S, Arrays.asList(a));
		Rule r2 = new Rule(S, Arrays.asList(and));
		Rule r3 = new Rule(S, Arrays.asList(lp,T,rp));
		Rule r4 = new Rule(T, Arrays.asList(S));
		Rule r5 = new Rule(T, Arrays.asList(T,comma,S));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(T,S));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(a,and,lp,rp,comma));
		NontermianlSymbol startSymbol = S;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		System.out.println(cfg.isLL1());
		cfg.removeLeftRecursion();
		System.out.println(cfg.isLL1());
 
		for(Rule rule : cfg.rules) {
			System.out.println(rule.toString() + "\t\t" + cfg.SELECT(rule));
		}
	}

}

/*
		NontermianlSymbol S = new NontermianlSymbol("S");
		NontermianlSymbol Q = new NontermianlSymbol("Q");
		NontermianlSymbol R = new NontermianlSymbol("R");
		
		TerminalSymbol a = new TerminalSymbol('a');
		TerminalSymbol b = new TerminalSymbol('b');
		TerminalSymbol c = new TerminalSymbol('c');
		
		Rule r1 = new Rule(S, Arrays.asList(Q,c));
		Rule r2 = new Rule(S, Arrays.asList(c));
		Rule r3 = new Rule(Q, Arrays.asList(R,b));
		Rule r4 = new Rule(Q, Arrays.asList(b));
		Rule r5 = new Rule(R, Arrays.asList(S,a));
		Rule r6 = new Rule(R, Arrays.asList(a));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5,r6));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(R,Q,S));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(a,b,c));
		NontermianlSymbol startSymbol = S;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		System.out.println(cfg.isLL1());
		cfg.removeLeftRecursion();
		System.out.println(cfg.isLL1());
 
*/

/*
		NontermianlSymbol S = new NontermianlSymbol("S");
		NontermianlSymbol A = new NontermianlSymbol("A");
		NontermianlSymbol B = new NontermianlSymbol("B");
		NontermianlSymbol C = new NontermianlSymbol("C");
		NontermianlSymbol D = new NontermianlSymbol("D");
		
		TerminalSymbol a = new TerminalSymbol('a');
		TerminalSymbol b = new TerminalSymbol('b');
		TerminalSymbol c = new TerminalSymbol('c');
		
		
		Rule r1 = new Rule(S, Arrays.asList(A,B));
		Rule r2 = new Rule(S, Arrays.asList(b,C));
		Rule r3 = new Rule(A, Arrays.asList(epsilon));
		Rule r4 = new Rule(A, Arrays.asList(b));
		Rule r5 = new Rule(B, Arrays.asList(epsilon));
		Rule r6 = new Rule(B, Arrays.asList(a,D));
		Rule r7 = new Rule(C, Arrays.asList(A,D));
		Rule r8 = new Rule(C, Arrays.asList(b));
		Rule r9 = new Rule(D, Arrays.asList(a,S));
		Rule r10 = new Rule(D, Arrays.asList(c));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5,r6,r7,r8,r9,r10));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(S,A,B,C,D));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(a,b,c));
		NontermianlSymbol startSymbol = S;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		System.out.println(cfg.FIRST(A,B));
		System.out.println(cfg.FIRST(b,C));
		System.out.println(cfg.FIRST(epsilon));
		System.out.println(cfg.FIRST(A,D));
		
		System.out.println();
		
		System.out.println(cfg.FOLLOW(S));
		System.out.println(cfg.FOLLOW(A));
		System.out.println(cfg.FOLLOW(B));
		System.out.println(cfg.FOLLOW(C));
		System.out.println(cfg.FOLLOW(D));
		
		System.out.println();
		for(Rule rule : rules) {
			System.out.println(cfg.SELECT(rule));
		}
*/