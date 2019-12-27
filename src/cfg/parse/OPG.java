package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import cfg.CFG;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;

public class OPG extends ButtonUpParsing {

	public static final char EMPTY = 0;
	//等于优先关系
	public static final char EQUAL = 'e';
	//小于优先关系
	public static final char LESS = 'l';
	//大于优先关系
	public static final char GREATER = 'g';
	
	//输入串的结束符号#
	private static final Symbol end = CFG.over;
	//算符优先关系表
	private char[][] matrix;
	//产生式右侧对左侧的映射，即规约
	Set<List<Class<? extends Symbol>>> reduce = new HashSet<>();
	
	
	private static final NontermianlSymbol N = new NontermianlSymbol("N");
	/**
	 *
	 * @param cfg
	 */
	public OPG(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
		//只考虑终结符之间的优先关系
		int size = cfg.getTerminals().size();
		//结束符号#
		matrix = new char[size][size];
		makeMatrix();
		makedReduce();
	}
	
	//构造reduce
	private void makedReduce() {
		for(Rule rule:cfg.getRules()) {
			List<Class<? extends Symbol>> list = new ArrayList<>();
			for(Symbol symbol : rule.getRight()) {
				list.add(symbol.getClass());
			}
			reduce.add(list);
		}
	}
	
	//构造算符优先关系表
	private void makeMatrix() {
		//辅助构造优先关系矩阵
		//help[0] 为FIRSTVT集，help[1]为LASTVT集
		Set<Symbol>[][] helper = makeHelper();
		//make matrix
		for(Rule rule : cfg.getRules()) {
			List<Symbol> right = rule.getRight();
			//检查每个产生式的任意两个相邻元素
			for(int i=0,j=1;j<right.size();i++,j++) {
				int indexi = getMatrixIndex(right.get(i));
				int indexj = getMatrixIndex(right.get(j));
				//equal
				if(TerminalSymbol.class.isInstance(right.get(i))) {
					if(TerminalSymbol.class.isInstance(right.get(j))) {
						//A -> ...ab...
						matrix[indexi][indexj] = EQUAL;
					}else {
						if(j+1 < right.size()
								&& TerminalSymbol.class.isInstance(right.get(j+1))) {
							//A -> ...aBb...
							int indexk = getMatrixIndex(right.get(j+1));
							matrix[indexi][indexk] = EQUAL;
						}
					}
				}
				//less
				//right.get(j)必须是非终结符
				if(NontermianlSymbol.class.isInstance(right.get(j))) {
					Set<Symbol> firstvt = helper[0][getHelperIndex(right.get(j))];
					//right.get(i)小于head集中的每个元素
					//A -> ...aB...
					for(Symbol symbol:firstvt) {
						int indexh = getMatrixIndex(symbol);
						matrix[indexi][indexh] = LESS;
					}
				}
				//greater
				//right.get(i)必须是非终结符
				if(NontermianlSymbol.class.isInstance(right.get(i))) {
					if(TerminalSymbol.class.isInstance(right.get(j))) {
						Set<Symbol> lastvt = helper[1][getHelperIndex(right.get(i))];
						//lastvt集中的每个元素大于right.get(j)
						//A -> ...Bb...
						for(Symbol symbol : lastvt) {
							int indexh = getMatrixIndex(symbol);
							matrix[indexh][indexj] = GREATER;
						}
					}
				}
			}
		}
	}
	
	//获取一个文法符号在关系矩阵中的索引
	private int getMatrixIndex(Symbol symbol) {
		//#规定在matrix.length-1的地方
		return cfg.getTerminals().indexOf(symbol);
	}
	
	private int getHelperIndex(Symbol symbol) {
		return cfg.getNonterminals().indexOf(symbol);
	}
	
	//构造辅助构造关系矩阵的FIRSTVT集和LASTVT集
	private Set<Symbol>[][] makeHelper() {
		@SuppressWarnings("unchecked")
		Set<Symbol>[][] helper = new Set[2][cfg.getNonterminals().size()];
		Map<NontermianlSymbol, List<Rule>> map = cfg.ruleGroupByNonterminal();
		//make helper
		//对每个非终结符生成FIRSTVT集和LASTVT集
		for(NontermianlSymbol nontermianlSymbol : cfg.getNonterminals()) {
			int index = getHelperIndex(nontermianlSymbol);
			helper[0][index] = new HashSet<>();
			helper[1][index] = new HashSet<>();
			//辅助队列
			Queue<Rule> queue = new LinkedList<Rule>();
			//make FIRSTVT
			queue.addAll(map.get(nontermianlSymbol));   
			//
			boolean[] marked = new boolean[cfg.getNonterminals().size()];
			marked[index] = true;
			while(!queue.isEmpty()) {
				Rule rule = queue.remove();
				List<Symbol> right = rule.getRight();
				Symbol first = right.get(0);
				if(NontermianlSymbol.class.isInstance(first)) {
					//
					if(right.size() > 1) {
						Symbol second = right.get(1);
						if(TerminalSymbol.class.isInstance(second)) {
							//B -> Cb...
							helper[0][index].add(second);
						}
					}
					if(!marked[cfg.getNonterminals().indexOf(first)]) {
						//如果是非终结符的话，需要继续探索
						marked[cfg.getNonterminals().indexOf(first)] = true;
						queue.addAll(map.get(first));
					}	

				}else {
					//只加入终结符
					//B -> b...
					helper[0][index].add(first);
				}
			}
			//make LASTVT
			//此时队列为空，则加入起始元素
			queue.addAll(map.get(nontermianlSymbol));   
			marked = new boolean[cfg.getNonterminals().size()];
			marked[index] = true;
			while(!queue.isEmpty()) {
				Rule rule = queue.remove();
				List<Symbol> right = rule.getRight();
				Symbol last = right.get(right.size()-1);
				if(NontermianlSymbol.class.isInstance(last)) {
					//
					if(right.size() > 1) {
						Symbol secondLast = right.get(right.size()-2);
						if(TerminalSymbol.class.isInstance(secondLast)) {
							//B -> ...bC
							helper[1][index].add(secondLast);
						}
					}
					if(!marked[cfg.getNonterminals().indexOf(last)]) {
						//如果是非终结符的话，需要继续探索
						marked[cfg.getNonterminals().indexOf(last)] = true;
						queue.addAll(map.get(last));
					}	

				}else {
					//只加入终结符
					//B -> ...b
					helper[1][index].add(last);
				}
			}
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
			TerminalSymbol si = getFirstTerminal(stack);
			Symbol sj = stream.get(0);
			
			//将输入流中的每个符号逐个存入分析栈中，直到遇到栈顶符号大于下一个待输入的符号
			if(matrix[getMatrixIndex(si)][getMatrixIndex(sj)] == LESS) {
				stack.add(stream.remove(0));
			}else if(matrix[getMatrixIndex(si)][getMatrixIndex(sj)] == GREATER) {
				//此刻在句柄尾，向左在栈中找句柄的头符号
				List<Class<? extends Symbol>> list = new LinkedList<>();

				int index=-1;
				TerminalSymbol pre = (TerminalSymbol) sj;
				ListIterator<Symbol> it = stack.listIterator(stack.size());
				while(it.hasPrevious()) {
					Symbol symbol = it.previous();
					if(TerminalSymbol.class.isInstance(symbol)) {
						if((matrix[getMatrixIndex(symbol)][getMatrixIndex(pre)] == LESS)) {
							index=stack.indexOf(symbol);
							break;
						}else {
							pre = (TerminalSymbol) symbol;
						}
					}
				}
				int count = stack.size()-index-1;
				for(int i=0;i<count;i++) {
					Symbol symbol = stack.pop();
					list.add(TerminalSymbol.class.isInstance(symbol)?TerminalSymbol.class:NontermianlSymbol.class);
				}
				
				//用相应的左部替代句柄
				if(reduce.contains(list))
					stack.add(N);
				else 
					return false;
			}else if(matrix[getMatrixIndex(si)][getMatrixIndex(sj)] == EQUAL){
				stack.add(stream.remove(0));
			}else {
				//未知关系，出错
				return false;
			}
			//当规约完输入符号串，栈中只剩文法开始符号时结束
			//#存在与stream和stack中
		}while(!(stream.get(0)==end && stack.get(0)==end && stack.get(1)==N && stack.size() == 2));
		
		return true;
	}
	
	private TerminalSymbol getFirstTerminal(Stack<Symbol> stack) {
		ListIterator<Symbol> it = stack.listIterator(stack.size());
		while(it.hasPrevious()) {
			Symbol symbol = it.previous();
			if(TerminalSymbol.class.isInstance(symbol)) {
				return (TerminalSymbol)symbol;
			}
		}
		return null;
	}
	

	
	public static void main(String[] args) {
		OPG opg = new OPG(CFG.makeCFG(
				"E1 -> # E # \n"
				+ "E -> E + T \n"
				+ "E -> T \n"
				+ "T -> T * F \n"
				+ "T -> F \n"
				+ "F -> P ^ F \n"
				+ "F -> P \n"
				+ "P -> ( E ) \n"
				+ "P -> i", "E1"));
		TerminalSymbol i = new TerminalSymbol("i");
		TerminalSymbol plus = new TerminalSymbol("+");
		@SuppressWarnings("unused")
		TerminalSymbol times = new TerminalSymbol("*");
		TerminalSymbol lp = new TerminalSymbol("(");
		TerminalSymbol rp = new TerminalSymbol(")");
		System.out.println(opg.parse(Arrays.asList(lp,i,plus,i,rp)));
	}
	
}
