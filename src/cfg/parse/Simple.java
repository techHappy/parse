package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import util.Displays;

/**
 * 自顶向上分析中的简单优先法
 * 简单优先法的基本思想是对一个文法按照一定规则求出该文法的所有符号之间的优先关系，
 * 并且根据这个关系确定规约过程中的句柄
 * 
 * 优点：准确、规范
 * 缺点：分析效率低、实用价值不大
 * @author 90946
 *
 */
public class Simple extends ButtonUpParsing {

	public static final char EMPTY = 0;
	//等于优先关系
	public static final char EQUAL = 'e';
	//小于优先关系
	public static final char LESS = 'l';
	//大于优先关系
	public static final char GREATER = 'g';
	
	//输入串的结束符号#
	private static final Symbol end = CFG.over;
	//简单优先关系矩阵
	private Character[][] matrix;
	//产生式右侧对左侧的映射，即规约
	Map<List<Symbol>,NontermianlSymbol> reduce = new HashMap<List<Symbol>, NontermianlSymbol>();
	
	/**
	 *
	 * @param cfg
	 */
	public Simple(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
		int size = cfg.getNonterminals().size() + cfg.getTerminals().size();
		matrix = new Character[size][size];
		makeMatrix();
		makedReduce();
		displayTable();
	}
	
	public void displayTable() {
		List<Symbol> list = new ArrayList<>(cfg.getNonterminals());
		list.addAll(cfg.getTerminals());
		Displays.displayTable(
				matrix, 
				list, 
				list, 
				"关系矩阵");
	}
	
	//构造reduce
	private void makedReduce() {
		for(Rule rule:cfg.getRules()) {
			reduce.put(rule.getRight(), rule.getLeft());
		}
	}
	
	//构造简单优先关系矩阵
	private void makeMatrix() {
		//辅助构造优先关系矩阵
		//help[0] 为HEAD集，help[1]为LAST集
		Set<Symbol>[][] helper = makeHelper();
		//make matrix
		for(Rule rule : cfg.getRules()) {
			List<Symbol> right = rule.getRight();
			//检查每个产生式的任意两个相邻元素
			for(int i=0,j=1;j<right.size();i++,j++) {
				int indexi = getMatrixIndex(right.get(i));
				int indexj = getMatrixIndex(right.get(j));
				//equal
				//两个相邻文法元素必有相等关系
				matrix[indexi][indexj] = EQUAL;
				//less
				//right.get(j)必须是非终结符
				if(NontermianlSymbol.class.isInstance(right.get(j))) {
					Set<Symbol> head = helper[0][getMatrixIndex(right.get(j))];
					//right.get(i)小于head集中的每个元素
					for(Symbol symbol:head) {
						int indexh = getMatrixIndex(symbol);
						matrix[indexi][indexh] = LESS;
					}
				}
				//greater
				//right.get(i)必须是非终结符
				if(NontermianlSymbol.class.isInstance(right.get(i))) {
					Set<Symbol> last = helper[1][getMatrixIndex(right.get(i))];
					//last集大于head集中的每个元素
					if(NontermianlSymbol.class.isInstance(right.get(j))) {
						Set<Symbol> head = helper[0][getMatrixIndex(right.get(j))];
						for(Symbol s1 : last) {
							for(Symbol s2 : head) {
								int index1 = getMatrixIndex(s1);
								int index2 = getMatrixIndex(s2);
								matrix[index1][index2] = GREATER;
							}
						}
					}
					//last集大于right.get(j)
					for(Symbol s:last) {
						int index1 = getMatrixIndex(s);
						matrix[index1][indexj] = GREATER;
					}
				}
			}
		}
		//填写#和其他文法符号的关系
		//开始符号
		matrix[cfg.getNonterminals().indexOf(cfg.getStartSymbol())][matrix.length-1] = GREATER;
		matrix[matrix.length-1][cfg.getNonterminals().indexOf(cfg.getStartSymbol())] = LESS;
		matrix[matrix.length-1][matrix.length-1] = EQUAL;
		Symbol start = cfg.getStartSymbol();
		//和开始符号有关的产生式
		for(Rule rule : cfg.ruleGroupByNonterminal().get(start)) {
			Symbol startSymbol = rule.getRight().get(0);
			Symbol lastSymbol = rule.getRight().get(rule.getRight().size()-1);
			
			if(NontermianlSymbol.class.isInstance(startSymbol)) {
				for(Symbol s : helper[0][getMatrixIndex(startSymbol)]) {
					matrix[matrix.length-1][getMatrixIndex(s)] = LESS;
				}
			}else {
				matrix[matrix.length-1][getMatrixIndex(startSymbol)] = LESS;
			}
			
			if(NontermianlSymbol.class.isInstance(lastSymbol)) {
				for(Symbol s : helper[1][getMatrixIndex(lastSymbol)]) {
					matrix[getMatrixIndex(s)][matrix.length-1] = GREATER;
				}
			}else {
				matrix[getMatrixIndex(lastSymbol)][matrix.length-1] = GREATER;
			}
		}
	}
	
	//获取一个文法符号在关系矩阵中的索引
	private int getMatrixIndex(Symbol symbol) {
		//#规定在matrix.length-1的地方
		
		if(NontermianlSymbol.class.isInstance(symbol)) {
			return cfg.getNonterminals().indexOf(symbol);
		}else {
			return cfg.getTerminals().indexOf(symbol)+cfg.getNonterminals().size();
		}
	}
	
	//构造辅助构造关系矩阵的HEAD集和LAST集
	private Set<Symbol>[][] makeHelper() {
		@SuppressWarnings("unchecked")
		Set<Symbol>[][] helper = new Set[2][cfg.getNonterminals().size()];
		Map<NontermianlSymbol, List<Rule>> map = cfg.ruleGroupByNonterminal();
		//make helper
		//对每个非终结符生成HEAD集和LAST集
		for(NontermianlSymbol nontermianlSymbol : cfg.getNonterminals()) {
			int index = cfg.getNonterminals().indexOf(nontermianlSymbol);
			helper[0][index] = new HashSet<>();
			helper[1][index] = new HashSet<>();
			//辅助队列
			Queue<Symbol> queue = new LinkedList<Symbol>();
			//make head
			queue.add(nontermianlSymbol);     
			while(!queue.isEmpty()) {
				Symbol symbol = queue.remove();
				//不管终结符还是非终结符，直接加入HEAD
				helper[0][index].add(symbol);
				if(NontermianlSymbol.class.isInstance(symbol)) {
					//如果是非终结符的话，需要继续探索
					for(Rule rule:map.get(symbol)) {
						Symbol s = rule.getRight().get(0);
						//不包含在队列中和HEAD中
						if(!helper[0][index].contains(s) && !queue.contains(s)) {
							queue.add(s);
						}
					}
				}
			}
			//移除自身
			helper[0][index].remove(nontermianlSymbol);
			//last
			//此时队列为空，则加入起始元素
			queue.add(nontermianlSymbol);
			while(!queue.isEmpty()) {
				Symbol symbol = queue.remove();
				//不管终结符还是非终结符，直接加入LAST
				helper[1][index].add(symbol);
				if(NontermianlSymbol.class.isInstance(symbol)) {
					//如果是非终结符的话，需要继续探索
					for(Rule rule:map.get(symbol)) {
						Symbol s = rule.getRight().get(rule.getRight().size()-1);
						//不包含在队列中和LAST中
						if(!helper[1][index].contains(s) && !queue.contains(s)) {
							queue.add(s);
						}
					}
				}
			}
			//移除自身
			helper[1][index].remove(nontermianlSymbol);
		}
		
		return helper;
	}

	@Override
	public boolean parse(List<Symbol> symbols) {
		// TODO Auto-generated method stub
		//分析栈
		Stack<Symbol> stack = new Stack<Symbol>();
		stack.add(end);
		//输入流
		List<Symbol> stream = new LinkedList<>(symbols);
		stream.add(end);
		do {
			Symbol sj = stream.get(0);
			Symbol si = stack.peek();
			
			//对于有些递归文法可能适用，有些不适用
			//处理ε符号，当cfg.removeLeftRecursion()会产生ε符号
			if(cfg.getTerminals().contains(CFG.epsilon) &&
					matrix[getMatrixIndex(si)][getMatrixIndex(CFG.epsilon)] == LESS &&
					matrix[getMatrixIndex(CFG.epsilon)][getMatrixIndex(sj)] == GREATER){
				stack.add(reduce.get(Arrays.asList(CFG.epsilon)));
				continue;
			}
			//将输入流中的每个符号逐个存入分析栈中，直到遇到栈顶符号大于下一个待输入的符号
			if(matrix[getMatrixIndex(si)][getMatrixIndex(sj)] == LESS) {
				stack.add(stream.remove(0));
			}else if(matrix[getMatrixIndex(si)][getMatrixIndex(sj)] == GREATER) {
				//此刻在句柄尾，向左在栈中找句柄的头符号
				List<Symbol> list = new LinkedList<>();
				list.add(stack.pop());
				while(matrix[getMatrixIndex(stack.peek())][getMatrixIndex(list.get(0))] == EQUAL) {
					list.add(0, stack.pop());
				}
				//用相应的左部替代句柄
				stack.add(reduce.get(list));
			}else if(matrix[getMatrixIndex(si)][getMatrixIndex(sj)] == EQUAL){
				stack.add(stream.remove(0));
			}else {
				//未知关系，出错
				return false;
			}
			//当规约完输入符号串，栈中只剩文法开始符号时结束
			//#存在与stream和stack中
		}while(!(stack.get(1)==cfg.getStartSymbol() && stream.get(0)==end));
		
		return true;
	}
	
	public static void main(String[] args) {
		NontermianlSymbol E = new NontermianlSymbol("E");
		NontermianlSymbol T = new NontermianlSymbol("T");
		
		TerminalSymbol plus = new TerminalSymbol("+");
		TerminalSymbol minus = new TerminalSymbol("-");
		TerminalSymbol i = new TerminalSymbol("i");
		
		Rule r1 = new Rule(E, Arrays.asList(E,plus,T));
		Rule r2 = new Rule(E, Arrays.asList(E,minus,T));
		Rule r3 = new Rule(T, Arrays.asList(i));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(E,T));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(i,minus,plus));
		NontermianlSymbol startSymbol = E;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
		Simple simple = new Simple(cfg);
//		System.out.println(simple.parse(Arrays.asList(a,b,plus,b,a)));
	}

}
//b((aa)a)b
/*
		NontermianlSymbol Z = new NontermianlSymbol("Z");
		NontermianlSymbol M = new NontermianlSymbol("M");
		NontermianlSymbol L = new NontermianlSymbol("L");
		
		TerminalSymbol a = new TerminalSymbol("a");
		TerminalSymbol b = new TerminalSymbol("b");
		TerminalSymbol lp = new TerminalSymbol("(");
		TerminalSymbol rp = new TerminalSymbol(")");
		
		Rule r1 = new Rule(Z, Arrays.asList(b,M,b));
		Rule r2 = new Rule(M, Arrays.asList(a));
		Rule r3 = new Rule(M, Arrays.asList(lp,L));
		Rule r4 = new Rule(L, Arrays.asList(M,a,rp));
		
		Set<Rule> rules = new HashSet<>(Arrays.asList(r1,r2,r3,r4));
		Set<NontermianlSymbol> nonterminals = new HashSet<>(Arrays.asList(Z,M,L));
		Set<TerminalSymbol> terminals = new HashSet<>(Arrays.asList(a,b,lp,rp));
		NontermianlSymbol startSymbol = Z;
		
		CFG cfg = new CFG(rules, nonterminals,terminals,startSymbol);
		
//		System.out.println(cfg.isLL1());

		Simple simple = new Simple(cfg);
		System.out.println(simple.parse(Arrays.asList(b,lp,lp,a,a,rp,a,rp,b)));
*/