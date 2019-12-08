package cfg;

public class TerminalSymbol implements Symbol{

	public final char val;
	public TerminalSymbol(char c) {
		super();
		this.val = c;
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(!TerminalSymbol.class.isInstance(obj))return false;
		TerminalSymbol terminalSymbol = (TerminalSymbol)obj;
		return val==terminalSymbol.val;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.valueOf(val);
	}
}
