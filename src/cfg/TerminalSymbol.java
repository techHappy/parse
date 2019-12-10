package cfg;

/**
 * 终结符
 * @author 90946
 *
 * <p>
 * 不变类,重载了{@code equals()}和{@code hashcode()}
 * <\p>
 */
public class TerminalSymbol implements Symbol{

	public final String val;
	public TerminalSymbol(String c) {
		super();
		this.val = c;
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(!TerminalSymbol.class.isInstance(obj))return false;
		TerminalSymbol terminalSymbol = (TerminalSymbol)obj;
		return val.equals(terminalSymbol.val);
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return val;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return val.hashCode();
	}
}
