/**
 * 
 */
package net.niconomicon.tile.source.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.niconomicon.tile.source.app.tiling.SQliteTileCreatorMultithreaded;

/**
 * @author Nicolas Hoibian
 * 
 */
public class SaveDialog extends JPanel {

	protected JTextField outputFileName;
	protected JTextField where;
	protected JButton browseOutput;

	FileDialog dirChooserOSX;
	JFileChooser dirChooser;

	JTextArea description;
	JTextField author;
	JTextField title;

	String currentTitle = "";
	String newLocation = null;

	public SaveDialog() {
		super();
		this.setLayout(new BorderLayout());
		init();
	}

	public void init() {
		GridBagConstraints c;
		int x, y;
		x = y = 0;
		JPanel option = new JPanel(new GridBagLayout());

		title = new JTextField("", 20);
		description = new JTextArea("", 5, 30);
		author = new JTextField(System.getProperty("user.name"), 20);

		// load from file name
		outputFileName = new JTextField("", 10);

		// could also load from the user's preferences
		String defaultDir = Ref.getDefaultDir();
		defaultDir = defaultDir == null ? System.getProperty("user.home") : defaultDir;
		where = new JTextField(defaultDir, 20);
		where.setEditable(false);

		browseOutput = new JButton("Choose Directory");
		browseOutput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread(new RootDirSetter());
				t.start();
			}
		});

		c = new GridBagConstraints();
		c.gridy = y++;
		c.gridx = x;
		c.anchor = c.LINE_END;
		option.add(new JLabel("Title :"), c);

		// //////////////

		c = new GridBagConstraints();
		c.gridy = y++;
		c.gridx = x;
		c.anchor = c.LINE_END;
		option.add(new JLabel("Save as :"), c);

		c = new GridBagConstraints();
		c.gridy = y++;
		c.gridx = x;
		c.anchor = c.LINE_END;
		option.add(new JLabel("In directory :"), c);

		x = 1;
		y = 0;
		c = new GridBagConstraints();
		c.gridy = y++;
		c.gridx = x;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		option.add(title, c);

		c = new GridBagConstraints();
		c.gridy = y++;
		c.gridx = x;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = c.LINE_START;
		option.add(outputFileName, c);

		c = new GridBagConstraints();
		c.gridy = y;
		c.gridx = x;
		c.anchor = c.LINE_START;
		option.add(where, c);

		c = new GridBagConstraints();
		c.gridy = y++;
		c.gridx = x + 1;
		option.add(browseOutput, c);

		this.add(option, BorderLayout.CENTER);
		// this.add(new JLabel("Save !"), BorderLayout.NORTH);
		initDialogs();
	}

	private void initDialogs() {
		String f = Ref.getDefaultDir();
		if (null == f) {
			f = System.getProperty("user.home");
		}
		File defDir = new File(f);
		// TODO check if java version > 1.5 otherwise it might crash :-(
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			System.setProperty("apple.awt.fileDialogForDirectories", "true");
			dirChooserOSX = new FileDialog(JFrame.getFrames()[0]);
		} else {

			dirChooser = new JFileChooser();
			dirChooser.setAcceptAllFileFilterUsed(false);
			dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			dirChooser.setDialogTitle("Choose directory to save the tile source");
			dirChooser.setCurrentDirectory(defDir);
		}
	}

	public String showDialog(Component parent, String currentLocation) {
		fillForm(currentLocation);
		boolean resOk = false;
		while (!resOk) {
			int result = JOptionPane.showOptionDialog(parent, this, "Save Displayable", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (JOptionPane.YES_OPTION == result) {
				resOk = save(currentLocation);
			} else {
				resOk = true;
			}
		}
		return newLocation;
	}

	public void fillForm(String currentLocation) {

		newLocation = null;
		try {
			currentTitle = SQliteTileCreatorMultithreaded.getTitle(currentLocation);
			title.setText(currentTitle);
			String suggestedFile = Ref.fileSansDot(currentLocation);
			try {
				if (Ref.isInTmpLocation(currentLocation)) {
					suggestedFile = suggestedFile.substring(0, suggestedFile.lastIndexOf("_")) + Ref.ext_db;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			this.outputFileName.setText(suggestedFile);
			this.where.setText(Ref.getDefaultDir());
			// init dialog
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean save(String originalFile) {
		String newPath = where.getText();
		if (null == newPath || "null".equals(newPath)) { return false; }
		if (null == outputFileName.getText() || "null".compareTo(outputFileName.getText()) == 0) { return false; }
		newPath = newPath.endsWith(File.separator) ? newPath : newPath + File.separator;

		Ref.setDefaultDir(newPath);
		newPath += outputFileName.getText();
		if (originalFile.compareTo(newPath) != 0) {
			File f = new File(originalFile);
			boolean res = f.renameTo(new File(newPath));
			if (!res) { return false; }
			newLocation = newPath;
		}
		// else{// yes ! no change to the location !

		if (null == title.getText() || "null".equals(title.getText())) { return false; }
		if (!currentTitle.equals(title.getText())) {
			SQliteTileCreatorMultithreaded.updateTitle(newPath, currentTitle, title.getText());
		}
		return true;
	}

	private class RootDirSetter implements Runnable {

		public void run() {
			// this block until ## is working on mac.
			if (null != dirChooserOSX) {
				if (getJavaMajorVersion() > 1.5) {
					dirChooserOSX.setModal(true);// only from java 1.6 :
													// setModalityType(ModalityType.APPLICATION_MODAL);
				}
				dirChooserOSX.setVisible(true);
				String dir = dirChooserOSX.getDirectory();
				String file = dirChooserOSX.getFile();
				System.out.println("Returned with directory : " + dir + file);
				if (null == dir || null == file) { return; }
				File f = new File(dir + file);
				String path;
				if (f.isDirectory()) {
					path = dir + file;
				} else {
					path = dir;
				}
				where.setText(path);
				where.setToolTipText("Going to save the Displayable in :" + path);
				// setRootDir(path);
				return;
			}
			// ##
			String s = " some file";

			int returnVal = dirChooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				// String s = dirChooser.getSelectedFile().getName();
				try {
					String path = dirChooser.getSelectedFile().getCanonicalPath();
					where.setText(path);
					// String wh = dirChooser.getSelectedFile().getAbsolutePath();

					// setRootDir(sourceChooser.getSelectedFile().getAbsolutePath());
				} catch (Exception ex) {
					outputFileName.setText(null);// "cannot Open File");
					where.setText(null);// Ref.cantOpenDir);
					ex.printStackTrace();
				}
			}
		}
	}

	public static double getJavaMajorVersion() {
		String javaVersion = System.getProperty("java.version");
		int p1 = javaVersion.indexOf('.');
		int p2 = javaVersion.indexOf('.', p1 + 1);
		double v = Double.parseDouble(javaVersion.substring(0, p2));
		System.out.println("java version : " + javaVersion + " a.k.a " + javaVersion.substring(0, p2) + " = " + v);
		return v;
		// System.out.println("");
	}

	public static void main(String[] args) {
		JFrame f = new JFrame();
		SaveDialog d = new SaveDialog();
		//
		f.setContentPane(d);
		f.pack();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}
}
