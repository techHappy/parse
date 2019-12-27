package util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

public class AnalyseTableModle extends AbstractTableModel{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7088133026047816418L;

	private Object[][] data;
	private String[] colName;
	private String[] rowName;
	
	public AnalyseTableModle(Object[][] data,Object[] colName,Object[] rowName) {
		// TODO Auto-generated constructor stub
		Objects.requireNonNull(data);
		Objects.requireNonNull(colName);
		Objects.requireNonNull(rowName);
		assert(data.length == rowName.length);
		assert(data[0].length == colName.length);
		assert(colName.length > 0);
		assert(rowName.length > 0);
		assert(data.length > 0);
		assert(data[0].length > 0);
		
		//column name
		this.colName = new String[colName.length+1];
		this.colName[0] = "";
		colName = Arrays.stream(colName).map(o->o.toString()).collect(Collectors.toList()).toArray(new String[] {});
		System.arraycopy(colName, 0, this.colName, 1, colName.length);
		//row name
		this.rowName = Arrays.stream(rowName).map(o->o.toString()).collect(Collectors.toList()).toArray(new String[] {});
		//data
		this.data = new Object[data.length][data[0].length+1];
		for(int i=0;i<data.length;i++) {
			this.data[i][0] = rowName[i];
			System.arraycopy(data[i], 0, this.data[i], 1, data[i].length);
		}
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		// TODO Auto-generated method stub
		return getValueAt(0, columnIndex)==null?String.class:getValueAt(0, columnIndex).getClass();
	}
	
	@Override
	public String getColumnName(int column) {
		// TODO Auto-generated method stub
		return colName[column].toString();
	}
	
	
	
	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return data.length;
	}

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return colName.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		Objects.checkIndex(rowIndex, getRowCount());
		Objects.checkIndex(columnIndex, getColumnCount());
		return data[rowIndex][columnIndex]==null?"":data[rowIndex][columnIndex];
	}
	
}
