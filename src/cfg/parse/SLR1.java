package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cfg.CFG;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;

public class SLR1 extends LR {

	public SLR1(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
		makeDfa();
		isSLR1();
		makeSLR1Table();
	}
	
	
	
	private void makeSLR1Table() {
		// TODO Auto-generated method stub
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
						Set<TerminalSymbol> follow = cfg.FOLLOW(rule.getLeft());
						for(TerminalSymbol terminalSymbol : follow) {
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



	//判断该文法是否是SLR1文法，若不是则抛出错误
	private void isSLR1() {
		for(Node state : states) {
			if(!check(state)) {
				List<Set<TerminalSymbol>> follows = new ArrayList<>();
				List<TerminalSymbol> A = new ArrayList<>();
				for(Rule rule : state.rules) {
					NontermianlSymbol left = rule.getLeft();
					List<Symbol> right = rule.getRight();
					int index = right.indexOf(delimiter);
					
					if(index == right.size()-1) {
						follows.add(cfg.FOLLOW(left));
					}else {
						if(TerminalSymbol.class.isInstance(right.get(index+1))) {
							A.add((TerminalSymbol)right.get(index+1));
						}
					}
				}
				
				for(int i=0;i<follows.size();i++) {
					for(int j=i+1;j<follows.size();j++) {
						Set<TerminalSymbol> set1 = new HashSet<>(follows.get(i));
						Set<TerminalSymbol> set2 = new HashSet<>(follows.get(j));
						set1.retainAll(set2);
						if(!set1.isEmpty()) {
							System.err.println();
							System.err.println(state.state+":");
							System.err.println(state.rules);
							System.err.println("A:"+A);
							System.err.println("follows:"+follows);
							throw new RuntimeException("该文法不是SLR1文法！");
						}
					}
				}
				
				for(int i=0;i<follows.size();i++) {
					Set<TerminalSymbol> Aset = new HashSet<>(A);
					Set<TerminalSymbol> set1 = new HashSet<>(follows.get(i));
					set1.retainAll(Aset);
					if(!set1.isEmpty()) {
						System.err.println();
						System.err.println(state.state+":");
						System.err.println(state.rules);
						System.err.println("A:"+A);
						System.err.println("follows:"+follows);
						throw new RuntimeException("该文法不是SLR1文法！");
					}
				}
			}
		}
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
		
		Arrays.deepToString(ACTION);
		Arrays.deepToString(GOTO);
//		System.out.printf("%-3s%-30s%-30s%-30s%-10s%-5s\n","步骤","状态栈","符号栈","输入流","ACTION","GOTO" );
//		int j=0;
		do {
			Symbol transfer = stream.get(0);
			String act = ACTION[stateStack.peekLast()][cfg.getTerminals().indexOf(transfer)];
			
			//
			if(act==null)return false;
			
			String[] parts = act.split(" ");
			switch(parts[0]) {
			case "acc":
				//接受
//				System.out.printf("%-3s%-30s%-30s%-5s%-5s%-5s\n",j++,stateStack,symbolStack,stream,"acc","" );
				return true;
			case "S":
				//移入
				stream.remove(0);
				symbolStack.add(transfer);
				stateStack.add(Integer.parseInt(parts[1]));
//				System.out.printf("%-3s%-30s%-30s%-5s%-5s%-5s\n",j++,stateStack,symbolStack,stream,act,"" );
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
//				System.out.printf("%-3s%-30s%-30s%-5s%-5s%-5s\n",j++,stateStack,symbolStack,stream,act,nextState );
				break;
			}
			
		}while(true);
	}
	
	public static void main(String[] args) {
		CFG cfg = CFG.makeCFG(
				"S -> a A d \n"
				+ "S -> b A c \n"
				+ "S -> a e c\n"
				+ "S -> b e d\n"
				+ "A -> e", "S");
		@SuppressWarnings("unused")
		
		SLR1 slr1 = new SLR1(cfg);
	}

}
