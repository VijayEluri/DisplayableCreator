/**
 * 
 */
package net.niconomicon.tile.source.app.viewer;

import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * @author Nicolas Hoibian
 * 
 */
public class TilingPreview extends JPanel {

	List<String> tileBuffer;
	List<String> doneTiles;

	ImageIcon icon;

	public static final int size = 500;
	

	public TilingPreview() {
		tileBuffer = new LinkedList<String>();
		doneTiles = new LinkedList<String>();
	}

	public void setMiniature(ImageIcon mini) {
		icon = mini;
		repaint();
	}

	public void addTile(String tileCoords) {
		synchronized (tileBuffer) {
			tileBuffer.add(tileCoords);
		}
	}

	private class monitor implements Runnable {
		public void run() {
			while (true) {
				synchronized (tileBuffer) {
					for (String tile : tileBuffer) {
						doneTiles.add(tile);
					}
					tileBuffer.clear();
				}
				repaint();
				try {
					Thread.sleep(100);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public void doingLevel(int level) {}

	/*
	protected void paintComponent(Graphics arg0) {
		super.paintComponent(arg0);
		Graphics2D g2d = (Graphics2D) arg0;
		Dimension d = this.getSize();
		
	}*/

}
