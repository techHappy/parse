package cfg.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cfg.CFG;
import cfg.LR1Rule;
import cfg.Rule;
import cfg.Symbol;
import cfg.TerminalSymbol;

public class LALR1 extends LR1 {

	public LALR1(CFG cfg) {
		super(cfg);
		// TODO Auto-generated constructor stub
		merge();
		makeLALR1Table();
		displayTable();
	}
	
	private void makeLALR1Table() {
		// TODO Auto-generated method stub
		super.makeLR1Table();
	}

	/**
	 * 合并同心集
	 */
	private void merge() {
		new ArrayList<>();
		//为每个项目集生成一个集合
		List<Set<Node>> sameHeartList = 
				states
				.stream()
				.map(n->new HashSet<>(Arrays.asList(n)))
				.collect(Collectors.toList());
		//合并同心集到一个集合
		for(Node state : states) {
			for(Set<Node> sameHeart : sameHeartList) {
				if(sameHeart.toArray(new Node[] {})[0].equalsInHeart(state)) {
					sameHeart.add(state);
					break;
				}
			}
		}
		//去除没有合并的同心集
		 sameHeartList
					= sameHeartList
					.stream()
					.filter(s->{return s.size()>1;})
					.collect(Collectors.toList());
		 //check
		sameHeartList.stream()
					.forEach(s->{checkReduceCollison(s);}); 
		//展平sameHeartList
		List<Node> flatSameHeartList 
		= sameHeartList
			.stream()
			.flatMap(s->{return s.stream();}) 
			.collect(Collectors.toList());
		//得到nonSameHeartList
		List<Node> nonSameHeartList = states.stream()
											.filter(s->{
												return !flatSameHeartList.contains(s);
											})
											.collect(Collectors.toList());
											

		//合并一个集合的同心集
		Function<Integer,List<Set<TerminalSymbol>>> function =
				(n) -> {
					List<Set<TerminalSymbol>> list = new ArrayList<>();
					for(int i=0;i<n;i++)list.add(new HashSet<>());
					return list;
				};
		
		List<Node> combinedSameHeartList
		=  sameHeartList
			.stream()
			.map(set->{
				Node oldNode = (Node) set.toArray()[0];
				Node newNode = new Node();
				newNode.transfers = oldNode.transfers;
				newNode.nexts = oldNode.nexts;
				
				int c = oldNode.initialRules.size();
				List<Set<TerminalSymbol>> newForwardList = 
				set.stream()
				.map(n->{ List<Set<TerminalSymbol>> list =
					n.initialRules
					.stream()
					.map(r->((LR1Rule)r)
							.getForwards())
					.collect(Collectors.toList());
					return list;})
				.reduce(function.apply(c),(a,s)->{
					for(int i=0;i<a.size();i++) {
						a.get(i).addAll(s.get(i));
					}
					return a;
				});
				
				List<Rule> newInitialRules = new ArrayList<>();
				for(int i=0;i<newForwardList.size();i++) {
					Rule oldRule = oldNode.initialRules.get(i);
					newInitialRules.add(
							new LR1Rule(
									oldRule.getLeft(), 
									oldRule.getRight(), 
									newForwardList.get(i)));
				}
				newNode.initialRules = newInitialRules;
				newNode.rules = closure(newInitialRules);
				
				return newNode;
			})
			.collect(Collectors.toList());
		//
		states = new ArrayList<>();
		states.addAll(nonSameHeartList);
		states.addAll(combinedSameHeartList);
		//重新分配ID
		reallocateId(states);
		//重新连接
		Map<Node, Node> map = new HashMap<LR.Node, LR.Node>();
		for(int i=0;i<combinedSameHeartList.size();i++) {
			for(Node s : sameHeartList.get(i)) {
				map.put(s, combinedSameHeartList.get(i));	
			}				
		}
		
		for(Node s1 : states) {
			for(int i=0;i<s1.nexts.size();i++) {
				Node s2 = s1.nexts.get(i);
				s1.nexts.set(i, map.getOrDefault(s2, s2));
			}
		}
		
	}
	
	
	protected boolean checkReduceCollison(Set<Node> s) {
		List<LR1Rule> list = new ArrayList<>();
		//寻找规约项目
		for(Node state : s) {
			for(Rule rule : state.rules) {
				List<Symbol> right = rule.getRight();
				int index = right.indexOf(delimiter);
				
				if(index == right.size()-1) {
					list.add((LR1Rule)rule);
				}
			}
		}
		//查找可能的规约冲突
		for(int i=0;i<list.size();i++) {
			LR1Rule r1 = list.get(i);
			Set<TerminalSymbol> set1 = new HashSet<>(r1.getForwards());
			for(int j=i+1;j<list.size();j++) {
				LR1Rule r2 = list.get(j);
				Set<TerminalSymbol> set2 = r2.getForwards();
				set1.retainAll(set2);
				if(!set1.isEmpty()) {
					err();
					return false;
				}
			}
		}
		return true;
	}
	
	private void err() {
		throw new RuntimeException("存在规约-规约冲突！");
	}

	public static void main(String[] args) {
		CFG cfg = CFG.makeCFG(
				"S -> B B \n"
				+ "B -> a B \n"
				+ "B -> b\n", "S");
		TerminalSymbol a = new TerminalSymbol("a");
		TerminalSymbol b = new TerminalSymbol("b");
		new LALR1(cfg);
	}
}
