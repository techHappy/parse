package cfg.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cfg.CFG;
import cfg.NontermianlSymbol;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;
import cfg.parse.LR0.Node;

public class SLR1 extends LR0 {

	public SLR1(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
	}
	
	//判断该文法是否是SLR1文法，若不是则抛出错误
	public void isSLR1() {
		boolean slr1 = true;
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
				
				System.err.println();
				System.err.println(state.state+":");
				System.err.println(state.rules);
				System.err.println("A:"+A);
				System.err.println("follows:"+follows);
			}
		}
		if(!slr1)throw new RuntimeException("该文法不是SLR1文法！");
	}

}
