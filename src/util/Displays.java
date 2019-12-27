package util;

import java.awt.Dimension;
import java.awt.Font;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.sun.imageio.stream.StreamCloser;

public class Displays {

	private Displays() {
		// TODO Auto-generated constructor stub
	}

	private static int tableOff = 0;
	public static <R,C> void displayTable(Object[][] tableData,List<R> rowName,List<C> colName,String title) {
		SwingUtilities.invokeLater(()->{
			JFrame frame = new JFrame();
			frame.setPreferredSize(new Dimension(600,600));
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setTitle(title);
			frame.setLocation(tableOff, tableOff);
			tableOff += 50;
			
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setPreferredSize(new Dimension(600,600));
			TableModel tableModel = new AnalyseTableModle(
					tableData, 
					colName.toArray(), 
					rowName.toArray());
			JTable table = new JTable(tableModel);
			table.setFont(new Font("宋体", Font.PLAIN, 20));
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(table);
			panel.add(scrollPane);
			
			frame.add(panel);
			
			frame.pack();
			frame.setVisible(true);
		});
	}

	public static void main(String[] args) {
		displayTable(
				new String[][] {{"1","2"},{"3","4"}}, 
				List.of(new String[] {"a","b"}), 
				List.of(new String[] {"c","d"}),
				"test");
		
	}
}

