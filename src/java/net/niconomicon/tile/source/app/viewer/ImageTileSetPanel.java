/**
 * 
 */
package net.niconomicon.tile.source.app.viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import net.niconomicon.tile.source.app.Ref;
import net.niconomicon.tile.source.app.TileCreatorApp;
import net.niconomicon.tile.source.app.viewer.actions.FlipAndAddAction;
import net.niconomicon.tile.source.app.viewer.actions.TileLoader;

/**
 * @author niko
 * 
 */
public class ImageTileSetPanel extends JPanel {
	Connection mapDB;
	public static final int tileSize = 192;

	public static final String getTilesInRange = "select * from tiles_0_0 where x >= ? and x <= ? and y >=? and y <=? and z=?";

	PreparedStatement tilesInRange;

	// public Map<String, byte[]> cache;
	public ConcurrentHashMap<String, BufferedImage> cache;
	int maxX = 0;
	int maxY = 0;
	int zoom = 0;

	ExecutorService exe;
	ExecutorService eye;
	JToolBar toolBar;

	public ImageTileSetPanel() {
		super();
		cache = new ConcurrentHashMap<String, BufferedImage>();
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		exe = Executors.newFixedThreadPool(TileCreatorApp.ThreadCount);
		eye = Executors.newFixedThreadPool(TileCreatorApp.ThreadCount / 2);
		toolBar = new JToolBar("Zoom", JToolBar.HORIZONTAL);

		JButton zP = new JButton("+");
		zP.addActionListener(new ZoomAction());
		toolBar.add(zP);
		JButton zM = new JButton("-");
		zM.addActionListener(new ZoomAction());
		toolBar.add(zM);

	}

	public void setTileSource(String tileSourcePath) {
		// this.getParent().add(toolBar);
		if (cache != null) {
			cache.clear();
		}
		try {
			System.out.println("trying to open the map : " + tileSourcePath);
			mapDB = DriverManager.getConnection("jdbc:sqlite:" + tileSourcePath);
			mapDB.setReadOnly(true);

			Statement statement = mapDB.createStatement();
			zoom = 0;
			ResultSet rs = statement.executeQuery("select * from " + Ref.layers_infos_table_name + " where zoom=" + zoom);

			while (rs.next()) {
				int width = rs.getInt("width");
				int height = rs.getInt("height");
				maxX = rs.getInt("tiles_x");
				maxY = rs.getInt("tiles_y");

				this.setSize(width, height);
				this.setMinimumSize(new Dimension(width, height));
				this.setPreferredSize(new Dimension(width, height));
			}
			System.out.println("caching ....");
			for (int i = 0; i < maxY; i++) {
				TileLoader loader = new TileLoader(mapDB, i, zoom, cache, eye);
				exe.execute(loader);
			}
			exe.awaitTermination(2, TimeUnit.MINUTES);
			revalidate();
			repaint();
			eye.awaitTermination(2, TimeUnit.MINUTES);
			System.out.println("fully cached !");
			revalidate();
			repaint();
		} catch (Exception ex) {
			System.err.println("ex for map : " + tileSourcePath);
			ex.printStackTrace();
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		// System.out.println("paintComponent");
		Graphics2D g2 = (Graphics2D) g;
		Rectangle r = g2.getClipBounds();
		int tileXa = r.x / tileSize;
		int tileXb = tileXa + (int) (((double) r.width / (double) tileSize)) + 2;
		int tileYa = r.y / tileSize;
		int tileYb = tileYa + (int) (((double) r.height / (double) tileSize)) + 1;

		// System.out.println("Painting between " + tileXa + "," + tileYa + "and " + tileXb + ", " + tileYb);
		try {
			int macYb = (maxY - 1 - tileYa);
			int macYa = (maxY - 1 - tileYb);

			macYa = tileYa;
			macYb = tileYb;
			for (int x = tileXa; x < tileXb; x++) {
				for (int y = macYa; y < macYb + 1; y++) {
					BufferedImage tile = cache.get(x + "_" + y + "_" + zoom);
					if (null != tile) {
						g2.drawImage(tile, x * tileSize, (y) * tileSize, null);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		g2.dispose();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String dir = "";// /Users/niko/tileSources/mapRepository/
		String file = "test.mdb";
		dir = "/Users/niko/tileSources/serving/";
		file = "busRomaCenter.mdb";
		if (args.length == 1) {
			dir = "";
			file = args[0];
		}
		ImageTileSetPanel mV = new ImageTileSetPanel();
		mV.setTileSource(dir + file);
		JScrollPane p = new JScrollPane(mV);
		JFrame frame = new JFrame("Map Viewer");
		frame.setContentPane(p);
		// frame.setContentPane(new JPanel(new BorderLayout()));
		// frame.getContentPane().add(mV, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public class ZoomAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			System.out.println("Action command : " + e.getActionCommand());
			if (e.getActionCommand().equals('+')) {
				System.out.println("zoom +");
				return;
			}
			if (e.getActionCommand().equals('-')) {
				System.out.println("zoom -");
				return;
			}
		}
	}
}
