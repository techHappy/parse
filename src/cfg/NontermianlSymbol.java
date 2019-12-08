package cfg;

public class NontermianlSymbol implements Symbol{
	
	public final String name;

	public NontermianlSymbol(String name) {
		super();
		this.name = name;
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(!NontermianlSymbol.class.isInstance(obj))return false;
		NontermianlSymbol nontermianlSymbol = (NontermianlSymbol)obj;
		return name.equals(nontermianlSymbol.name);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return name;
	}
}
