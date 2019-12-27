package cfg.parse;

import java.util.List;

import cfg.Symbol;

/**
 * 语法分析方法中的一种分析方式
 * @author 90946
 *
 */
public interface Parser {
	/**
	 * 解析输入的文法符号序列，并判断是否接受该输入流
	 * @param symbols 输入流
	 * @return 该文法是否接受该输入流
	 */
	public boolean parse(List<Symbol> symbols);
	
}
