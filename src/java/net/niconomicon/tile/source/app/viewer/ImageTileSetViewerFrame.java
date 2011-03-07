/**
 * 
 */
package net.niconomicon.tile.source.app.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import net.niconomicon.tile.source.app.tiling.SQliteTileCreatorMultithreaded;
import net.niconomicon.tile.source.app.viewer.ImageTileSetPanel.ZoomLevel;

/**
 * @author Nicolas Hoibian
 * 
 */
public class ImageTileSetViewerFrame extends JPanel {

	ImageTileSetPanel tileViewer;
	String tileSetLocation;
	JFrame viewerFrame;
	JLabel currentZoom;
	JLabel infos;
	JToolBar toolBar;

	public static ImageTileSetViewerFrame createInstance() {
		return new ImageTileSetViewerFrame(new ImageTileSetPanel());
	}

	private ImageTileSetViewerFrame(ImageTileSetPanel tileViewer) {
		super();
		this.tileViewer = tileViewer;
		init();
	}

	private void init() {
		viewerFrame = new JFrame();
		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(tileViewer), BorderLayout.CENTER);

		this.setMinimumSize(new Dimension(400, 400));
		viewerFrame.setContentPane(this);
		viewerFrame.setMinimumSize(new Dimension(400, 400));
		viewerFrame.setLocation(400, 200);
		viewerFrame.setSize(500, 500);
		this.setPreferredSize(new Dimension(500, 500));

		toolBar = new JToolBar("Zoom", JToolBar.HORIZONTAL);
		currentZoom = new JLabel();
		infos = new JLabel();

		JButton zP = new JButton("+");
		zP.addActionListener(new ZoomAction());
		toolBar.add(zP);
		JButton zM = new JButton("-");
		zM.addActionListener(new ZoomAction());
		toolBar.add(zM);
		toolBar.add(infos);
		toolBar.add(currentZoom);
		this.add(toolBar, BorderLayout.NORTH);
	}

	public void setTileSet(String tileSetLocation) {
		System.out.println("setting tile set");
		infos.setText(" Size : ? px * ? px. ");
		currentZoom.setText("Current zoom : ");
		this.tileSetLocation = tileSetLocation;
		try {
			String title = SQliteTileCreatorMultithreaded.getTitle(tileSetLocation);
			viewerFrame.setTitle(title);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		viewerFrame.pack();
		viewerFrame.setVisible(true);

		Thread t = new Thread(new TileSourceSetter(tileSetLocation));
		t.start();
	}

	private class TileSourceSetter implements Runnable {
		String tileSourceLocation;

		public TileSourceSetter(String location) {
			tileSetLocation = location;
		}

		public void run() {
			tileViewer.setTileSource(tileSetLocation);
			currentZoom.setText("Current zoom : " + (tileViewer.getMaxZ() - tileViewer.currentLevel.z) + " / " + tileViewer.getMaxZ());
			ZoomLevel zl = tileViewer.getMaxInfo();
			infos.setText(" Size : " + zl.width + " px * " + zl.height + " px. ");
			tileViewer.revalidate();
			revalidate();
			viewerFrame.pack();
			viewerFrame.setVisible(true);

		}
	}

	public class ZoomAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			System.out.println("Action command : [" + e.getActionCommand() + "]");
			if (e.getActionCommand().equals("+")) {
				System.out.println("zoom +");
				tileViewer.incrZ();
			}
			if (e.getActionCommand().equals("-")) {
				System.out.println("zoom -");
				tileViewer.decrZ();
			}
			currentZoom.setText("Current zoom : " + (tileViewer.getMaxZ() - tileViewer.currentLevel.z) + "/" + tileViewer.getMaxZ());
		}
	}

}
