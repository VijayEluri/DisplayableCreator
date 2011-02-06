/**
 * 
 */
package net.niconomicon.tile.source.app.sharing;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.niconomicon.tile.source.app.Ref;
import net.niconomicon.tile.source.app.filter.DirOrTilesetFilter;
import net.niconomicon.tile.source.app.tiling.SQliteTileCreatorMultithreaded;
import net.niconomicon.tile.source.app.viewer.ImageTileSetViewerFrame;

/**
 * @author niko
 * 
 */
public class TilesetSharingPanel extends JPanel implements TableModelListener {

	boolean currentlySharing = false;
	SharingManager sharingManager;
	CheckBoxTileSetTable mapList;
	JSpinner portNumber;
	JLabel sharingStatus;
	String rootDir = "/Users/niko/Sites/testApp/mapRepository";

	ImageTileSetViewerFrame viewer;

	InetAddress localaddr;

	/**
	 * Stand alone main
	 * 
	 */
	public static void main(String[] args) {
		TilesetSharingPanel service = new TilesetSharingPanel(null);
		JFrame frame = new JFrame("Tileset Sharing Service");
		frame.setContentPane(service);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub
				((TilesetSharingPanel) ((JFrame) e.getSource()).getContentPane()).stopSharing();
				super.windowClosing(e);
			}
		});
		frame.setVisible(true);
	}

	public TilesetSharingPanel(ImageTileSetViewerFrame viewer) {
		this.viewer = viewer;
		init();
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e) {
		if (sharingManager.isSharing()) {
			sharingManager.setSharingList(mapList.getSelectedTilesSetFiles());
			// update the list of shared documents
		} else {
			// don't care ;-)
			return;
		}
	}

	public JPanel getDirSelectionPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(new JLabel("Import TileSets : "), BorderLayout.WEST);
		JButton b = new JButton("Choose TileSets or TileSet directory");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.setFileFilter(new DirOrTilesetFilter());
				fc.setMultiSelectionEnabled(true);
				int res = fc.showOpenDialog(TilesetSharingPanel.this);
				if (JFileChooser.APPROVE_OPTION == res) {
					File[] files = fc.getSelectedFiles();
					setSelectedFiles(files);
				}
			}
		});
		p.add(b, BorderLayout.EAST);
		return p;
	}

	public void init() {
		try {
			localaddr = InetAddress.getLocalHost();

			System.out.println("Local IP Address : " + localaddr);
			System.out.println("Local hostname   : " + localaddr.getHostName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		sharingManager = new SharingManager();
		mapList = new CheckBoxTileSetTable(viewer);

		mapList.getModel().addTableModelListener(this);
		mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// mapList.getSelectionModel().addListSelectionListener(this);
		this.setLayout(new BorderLayout());
		JPanel dirSelectorPanel = getDirSelectionPanel();
		this.add(dirSelectorPanel, BorderLayout.NORTH);
		// shared files
		// //////////////////////////////////////////
		this.add(new JScrollPane(mapList), BorderLayout.CENTER);
		JPanel options = new JPanel(new GridLayout(0, 1));
		// //////////////////////////////////////////
		// port number
		JPanel p = new JPanel(new GridLayout(0, 2));
		p.add(new JLabel("Image TileSet sharing port : "));
		portNumber = new JSpinner(new SpinnerNumberModel(Ref.sharing_port, 1025, 65536, 1));
		p.add(portNumber);
		options.add(p);
		// start sharing
		sharingStatus = new JLabel("Image TileSet Sharing status : [not running]");
		options.add(sharingStatus);
		JButton shareButton = new JButton("Start Image TileSet sharing");
		shareButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				currentlySharing = !currentlySharing;
				JButton b = (JButton) e.getSource();
				if (currentlySharing) {
					sharingStatus.setText("Image TileSet Sharing status : [starting ...]");
					sharingStatus.revalidate();
					startSharing();
					sharingStatus.setText("Image TileSet Sharing status : [running]");
					sharingStatus.setToolTipText("If the items do not appear quickly in the list, try accessing http://" + localaddr.getHostName() + ":" + sharingManager.port + "/ in your browser");
					b.setText("Stop Image TileSet sharing");
				} else {
					sharingStatus.setText("Image TileSet Sharing status : [stopping ...]");
					sharingStatus.revalidate();
					stopSharing();
					sharingStatus.setToolTipText("");
					sharingStatus.setText("Image TileSet Sharing status : [not running]");
					b.setText("Start Image TileSet sharing");
				}
			}
		});
		options.add(shareButton);
		this.add(options, BorderLayout.SOUTH);
	}

	public static List<String> getDBFilesInSubDirectory(File dir) {
		List<String> dbFiles = new ArrayList<String>();
		dbFiles.addAll(Arrays.asList(Ref.getAbsolutePathOfDBFilesInDirectory(dir)));
		for (File d : dir.listFiles()) {
			if (d.isDirectory()) {
				dbFiles.addAll(getDBFilesInSubDirectory(d));
			}
		}
		return dbFiles;
	}

	public void setSelectedFiles(File[] files) {
		if (null == files) { return; }// clear ?
		List<String> dbFiles = new ArrayList<String>();
		for (File f : files) {
			if (f.isDirectory()) {
				dbFiles.addAll(getDBFilesInSubDirectory(f));
			} else {
				if (f.getAbsolutePath().endsWith(Ref.ext_db)) {
					dbFiles.add(f.getAbsolutePath());
				}
			}
		}
		Collections.sort(dbFiles);
		// String[] children = Ref.getDBFiles(rootDir);
		Map<String, String> fileToTitle = new HashMap<String, String>();// getTilesetList(rootDir, children);
		for (String path : dbFiles) {
			try {
				System.out.println("Going to get the title from " + path);
				fileToTitle.put(path, SQliteTileCreatorMultithreaded.getTitle(path));
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		mapList.setData(fileToTitle);
		if (sharingManager.isSharing()) {
			sharingManager.setSharingList(mapList.getSelectedTilesSetFiles());
		}
	}

	public void startSharing() {
		// HashSet<String> sharedDB = new HashSet<String>();
		Collection<String> sharedMaps = mapList.getSelectedTilesSetFiles();
		System.out.println("should start sharing the maps");
		// generate the xml;
		try {
			sharingManager.setPort(((SpinnerNumberModel) portNumber.getModel()).getNumber().intValue());
			sharingManager.setSharingList(sharedMaps);
			sharingManager.startSharing();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void stopSharing() {
		try {
			sharingManager.stopSharing();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * @param fileLocation
	 *            This can be a temporary file.
	 */
	public void addTileSetToShare(String fileLocation, String title) {
		mapList.addTileSet(fileLocation, title);
	}

	public void updateTileSetLocation(String oldLocation, String newLocation) {
		mapList.updateLocation(oldLocation, newLocation);
	}

	public Map<String, String> getTilesetList(String rootDir, String[] maps) {
		// try {
		// Class.forName("org.sqlite.JDBC");
		// } catch (Exception ex) {
		// ex.printStackTrace();
		// }

		Map<String, String> fileToName = new HashMap<String, String>();
		if (!rootDir.endsWith(File.separator)) {
			rootDir += File.separator;
		}
		for (String string : maps) {
			try {
				String fileName = rootDir + string;
				System.out.println("trying to open the map : " + fileName);
				Connection mapDB = DriverManager.getConnection("jdbc:sqlite:" + fileName);
				mapDB.setReadOnly(true);
				Statement statement = mapDB.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 sec.
				ResultSet rs = statement.executeQuery("select " + Ref.infos_title + " from infos");
				while (rs.next()) {
					String name = rs.getString(Ref.infos_title);
					System.out.println("name : " + name);
					fileToName.put(fileName, name);
				}
				if (mapDB != null) mapDB.close();
			} catch (Exception ex) {
				System.err.println("ex for map : " + string);
				ex.printStackTrace();
			}
		}
		return fileToName;
	}
}
