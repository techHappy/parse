package cfg;

import java.util.ArrayList;
import java.util.List;

public class Rule {
	
	final NontermianlSymbol left;
	final List<Symbol> right;
	
	public Rule(NontermianlSymbol left, List<Symbol> right) {
		super();
		this.left = left;
		this.right = new ArrayList<Symbol>(right);
	}

	public NontermianlSymbol getLeft() {
		return left;
	}

	public List<Symbol> getRight() {
		return right;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(!Rule.class.isInstance(obj))return false;
		Rule rule = (Rule)obj;
		return left.equals(rule.left) && right.equals(rule.right);
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		for(Symbol symbol : right) {
			sb.append(symbol.toString());
		}
		return left+" -> "+sb;
	}

}
