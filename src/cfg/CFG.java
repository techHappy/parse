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
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CFG {
	//产生式序列
	private List<Rule> rules;
	
	/**
	 * 改变文法的产生式
	 * @param rules 更换的产生式
	 */
	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	//非终结符集
	private final List<NontermianlSymbol> nonterminals;
	//终结符集
	private final Set<TerminalSymbol> terminals;
	//开始符号
	private NontermianlSymbol startSymbol;
	
	private int[] nonCanInferEpsilon;
	
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
	public static final TerminalSymbol epsilon = new TerminalSymbol("ε");
	
	public static final TerminalSymbol over = new TerminalSymbol("#");
	
	//FIRST集
	private Set<TerminalSymbol>[] FIRST;  
	//FOLLOW集
	private Set<TerminalSymbol>[] FOLLOW;
	//SELECT集
	private Set<TerminalSymbol>[] SELECT;
	
	private boolean isLL1;
	
	/**
	 * 
	 * @param rules
	 * @param nonterminals
	 * @param terminals
	 * @param startSymbol
	 */
	public CFG(Set<Rule> rules, Set<NontermianlSymbol> nonterminals, Set<TerminalSymbol> terminals,
			NontermianlSymbol startSymbol) {
		super();
		this.rules = new ArrayList<>(rules);
		this.nonterminals = new ArrayList<>(nonterminals);
		this.terminals = new HashSet<>(terminals);
		this.startSymbol = startSymbol;
		
		caculateIsLL1();
	}
	
	/**
	 *  复制构造
	 * @param cfg
	 */
	public CFG(CFG cfg) {
		super();
		this.rules = new ArrayList<>(cfg.rules);
		this.nonterminals = new ArrayList<>(cfg.nonterminals);
		this.terminals = new HashSet<>(cfg.terminals);
		this.startSymbol = cfg.startSymbol;

		caculateIsLL1();
	}
	
	@SuppressWarnings("unchecked")
	private void caculateIsLL1() {
		nonCanInferEpsilon = getNonterminalOfInferringEpsilon();
		FIRST = new HashSet[nonterminals.size()];
		FOLLOW = new HashSet[nonterminals.size()];
		SELECT = new HashSet[rules.size()];
		calculateFIRST();
		caculateFOLLOW();
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
	
	//构造FIRST集
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
	
	//构造FOLLOW集
	private void caculateFOLLOW() {
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
	
	//构造SELECT集
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
		
	//计算能推导出epsilon的非终结符
	private int[] getNonterminalOfInferringEpsilon() {
		int[] canInferring = new int[nonterminals.size()];
		Arrays.fill(canInferring, UNDETERMINED);
		
		List<Rule> _rules = new LinkedList<Rule>(rules);
		
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
						//
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
								break;
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
	
	/**
	 * 根据给定的符号序列，返回其相应的FIRST集
	 * @param symbols 给定的符号序列
	 * @return 与symbols相应的FIRST集
	 */
	public Set<TerminalSymbol> FIRST(Symbol ... symbols){
		if(symbols.length == 0) return Stream.of(epsilon).collect(Collectors.toSet());
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
	
	/**
	 * 根据非终结符返回其相应的FOLLOW集
	 * @param nontermianlSymbol 非终结符
	 * @return 与nonterminalSymbol对应的FOLLOW集
	 */
	public Set<TerminalSymbol> FOLLOW(NontermianlSymbol nontermianlSymbol){			
		return FOLLOW[nonterminals.indexOf(nontermianlSymbol)];
	}
	
	/**
	 * 根据产生式返回其相应的SELECT集
	 * @param rule 产生式
	 * @return 与rule相对应的SElECT集
	 */
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
	
	/**
	 * 添加额外的产生式进该文法的产生式集
	 * 
	 * <p>
	 * 会改变该文法的终结符集和非终结符集
	 * 
	 * @param rule 额外的产生式
	 */
	public void addRule(Rule rule) {
		if(!rules.contains(rule)) {
			rules.add(rule);
			NontermianlSymbol left = rule.left;
			if(!nonterminals.contains(left))nonterminals.add(left);
			for(Symbol symbol:rule.right) {
				if(NontermianlSymbol.class.isInstance(symbol)) {
					//非终结符
					if(!nonterminals.contains(symbol))
						nonterminals.add((NontermianlSymbol)symbol);
				}else {
					//终结符
					if(!terminals.contains(symbol))
						terminals.add((TerminalSymbol)symbol);
				}
			}
		}
	}
	

	
	/**
	 * 根据给定的文法描述，构造出相应的CFG
	 * <p>
	 * 文法描述为一系列产生式，每一行一个产生式，产生式中的文法符号由空格隔开.
	 * 产生式的推导符不重要，但一定要与文法符号用空格隔开。左边只有一个非终结符。
	 * </p>
	 * 
	 * <p>
	 * {@code 	makeCFG("E -> T + T\n "
				+ "E -> T - T\n "
				+ "T -> F * F\n "
				+ "T -> F / F\n", "E");}
	 * </p>
	 * @param text 文法描述
	 * @param start 开始符号
	 * @return 构造出的CFG
	 */
	public static CFG makeCFG(String text,String start) {
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(text);
		
		Set<Rule> rules = new HashSet<>();
		Set<NontermianlSymbol> nonterminals = new HashSet<>();
		Set<TerminalSymbol> terminals;
		
		for(String t=null;scanner.hasNext();) {
			t=scanner.nextLine().trim();
			String[] parts = t.split("\\s+");
			NontermianlSymbol nontermianlSymbol = new NontermianlSymbol(parts[0]);
			nonterminals.add(nontermianlSymbol);
		}
		
		List<NontermianlSymbol> nontermianlList = new ArrayList<>(nonterminals);
		List<TerminalSymbol> terminalList = new ArrayList<>();
		
		scanner = new Scanner(text);
		for(String t=null;scanner.hasNext();) {
			t=scanner.nextLine().trim();
			String[] parts = t.split("\\s+");
			List<Symbol> right = new ArrayList<>();
			NontermianlSymbol left = nontermianlList.get(nontermianlList.indexOf(new NontermianlSymbol(parts[0])));
			//构造right
			for(int i=2;i<parts.length;i++) {
				NontermianlSymbol nontermianlSymbol=new NontermianlSymbol(parts[i]);
				if(!nonterminals.contains(nontermianlSymbol)) {
					//terminal
					TerminalSymbol terminalSymbol=new TerminalSymbol(parts[i]);
					if(!terminalList.contains(terminalSymbol)) {
						//未加入的terminal
						terminalList.add(terminalSymbol);
						right.add(terminalSymbol);
					}else {
						//已加入的terminal
						right.add(terminalList.get(terminalList.indexOf(terminalSymbol)));
					}
				}else {
					//nonterminal 直接加入
					right.add(nontermianlList.get(nontermianlList.indexOf(nontermianlSymbol)));
				}
			}
			rules.add(new Rule(left, right));
		}
		
		NontermianlSymbol startSymbol = nontermianlList.get(nontermianlList.indexOf(new NontermianlSymbol(start)));
		terminals = new HashSet<>(terminalList);
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		return cfg;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		makeCFG("E -> T + T\n "
				+ "E -> T - T\n "
				+ "T -> F * F\n "
				+ "T -> F / F\n", "E");
		
		
//		NontermianlSymbol procedure = new NontermianlSymbol("procedure");
//		NontermianlSymbol subprocedure = new NontermianlSymbol("subprocedure");
//		NontermianlSymbol constDefinition = new NontermianlSymbol("constDefinition");
//		NontermianlSymbol varDefinition = new NontermianlSymbol("varDefinition");
//		NontermianlSymbol statement = new NontermianlSymbol("statement");
//		NontermianlSymbol condition = new NontermianlSymbol("condition");
//		NontermianlSymbol expression = new NontermianlSymbol("expression");
//		NontermianlSymbol term = new NontermianlSymbol("term");
//		NontermianlSymbol factor = new NontermianlSymbol("factor");
		
		
//		NontermianlSymbol S = new NontermianlSymbol("S");
//		NontermianlSymbol T = new NontermianlSymbol("T");
//		
//		TerminalSymbol a = new TerminalSymbol("a");
//		TerminalSymbol and = new TerminalSymbol("^");
//		TerminalSymbol lp = new TerminalSymbol("(");
//		TerminalSymbol rp = new TerminalSymbol(")");
//		TerminalSymbol comma = new TerminalSymbol(",");
//		
//		Rule r1 = new Rule(S, Arrays.asList(a));
//		Rule r2 = new Rule(S, Arrays.asList(and));
//		Rule r3 = new Rule(S, Arrays.asList(lp,T,rp));
//		Rule r4 = new Rule(T, Arrays.asList(S));
//		Rule r5 = new Rule(T, Arrays.asList(T,comma,S));
//		
//		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4,r5));
//		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(T,S));
//		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(a,and,lp,rp,comma));
//		NontermianlSymbol startSymbol = S;
//		
//		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
//		
//		System.out.println(cfg.isLL1());
//		cfg.removeLeftRecursion();
//		System.out.println(cfg.isLL1());
// 
//		for(Rule rule : cfg.rules) {
//			System.out.println(rule.toString() + "\t\t" + cfg.SELECT(rule));
//		}
	}

}

/*
		NontermianlSymbol S = new NontermianlSymbol("S");
		NontermianlSymbol Q = new NontermianlSymbol("Q");
		NontermianlSymbol R = new NontermianlSymbol("R");
		
		TerminalSymbol a = new TerminalSymbol("a");
		TerminalSymbol b = new TerminalSymbol("b");
		TerminalSymbol c = new TerminalSymbol("c");
		
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
		
		TerminalSymbol a = new TerminalSymbol("a");
		TerminalSymbol b = new TerminalSymbol("b");
		TerminalSymbol c = new TerminalSymbol("c");
		
		
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