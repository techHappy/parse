package cfg;

import java.util.List;
import java.util.Set;

public class LR1Rule extends Rule {

	final Set<TerminalSymbol> forwards;
	public LR1Rule(NontermianlSymbol left, List<Symbol> right ,Set<TerminalSymbol> forwards) {
		super(left, right);
		// TODO Auto-generated constructor stub
		this.forwards = forwards;
	}
	public Set<TerminalSymbol> getForwards() {
		return forwards;
	}

	public boolean equalsInHeart(Rule rule) {
		if(!LR1Rule.class.isInstance(rule))return false;
		
		return super.equals(rule);
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		//!!!!!!!!!!!!!!!!!!!!!
		if(!Rule.class.isInstance(obj))return false;
		if(LR1Rule.class.isInstance(obj)) {
			LR1Rule lr1Rule = (LR1Rule)obj;
			return super.equals(obj) && forwards.equals(lr1Rule.forwards);
		}else {
			Rule rule = (Rule)obj;
			return left.equals(rule.left) && right.equals(rule.right);
		}
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString()+","+forwards;
	}
}
