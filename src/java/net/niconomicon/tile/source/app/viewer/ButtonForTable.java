/**
 * 
 */
package net.niconomicon.tile.source.app.viewer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * @author Nicolas Hoibian
 * 
 */
public class ButtonForTable extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {

	String lastValue = null;
	JButton ren;
	ImageTileSetViewer viewer;
	String defaultText;

	public ButtonForTable(ImageTileSetViewer viewer, String text) {
		defaultText = text;
		ren = new JButton(text);
		this.viewer = viewer;
	}

	public Object getCellEditorValue() {
		return lastValue;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		JButton b = ren;
		if (value == null) {
			b.setText(defaultText);
		} else {
			b.setText(value.toString());
		}
		return b;
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		lastValue = value.toString();
		JButton b = new JButton();
		b.addActionListener(this);
		b.setText(value.toString());
		return b;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (null != viewer) {
			viewer.setTileSet(lastValue);
		}
		System.out.println("Action performed. Presumably at row " + lastValue);
		// setTileSet()
	}
}
