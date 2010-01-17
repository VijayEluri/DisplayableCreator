package net.niconomicon.tile.source.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

public class SQliteTileCreator {
	Connection connection;

	public static final double MINIATURE_SIZE = 500;

	public static double ZOOM_FACTOR = 0.5;
	// public static int TILE_SIZE = 256;

	public static boolean CREATE_MINIATURE = true;

	public static int MAX_ZOOM_LEVEL = 0;

	public String tileSetID;
	public String name;
	public String title;
	public String description;

	public String author;
	public String source;

	long mapKey;
	long layerKey;
	int zIndex;
	int sourceWidth;
	int sourceHeigth;
	byte[] mini;
	byte[] thumb;

	public boolean doneCalculating = false;

	public PreparedStatement insertTile;

	/**
	 * an archive is a collection of maps. a map is a collection of layers. a layer has an area
	 * 
	 * @param archiveName
	 * @param fileSansDot
	 */
	public void initSource(String archiveName, String fileSansDot) {
		mapKey = -1;
		layerKey = -1;
		
		if (mapKey == -1) {
			mapKey = 0;// currently only one map per database
		}
		if (layerKey == -1) {
			layerKey = 0;// currently only one layer per map
		}
		
		// load the sqlite-JDBC driver using the current class loader
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return;
		}

		connection = null;
		try {
			// create a database connection
			connection = DriverManager.getConnection("jdbc:sqlite:" + archiveName);
			System.out.println("Archive name : " + archiveName);
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			statement.executeUpdate("drop table if exists infos");
			statement.executeUpdate("drop table if exists level_infos");
			statement.executeUpdate("drop table if exists tiles_" + mapKey);
			// 
			statement.executeUpdate("CREATE TABLE infos (title STRING, mapKey LONG, description STRING, author STRING, source STRING, date STRING, zindex LONG, " + "width LONG, height LONG," + "miniature BLOB,thumb BLOB)");
			// currently the layer name should be the same as the map name, as only one layer is supported
			statement.executeUpdate("CREATE TABLE layers_infos (" + "layerName STRING, mapKey LONG, zindex LONG, zoom  LONG, width LONG,height LONG, tiles_x LONG,tiles_y LONG, offset_x LONG, offset_y LONG)");
			statement.executeUpdate("CREATE TABLE tiles_" + mapKey + "_"+layerKey+ " (x LONG , y LONG, z LONG, data BLOB)");
			// Prepare most frequently used statement;
			String insertTiles = "insert into tiles_" + mapKey + "_" + layerKey + " values( ?, ?, ?, ?)";
			insertTile = connection.prepareStatement(insertTiles);
			insertTile.clearParameters();
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void finalizeFile() {
		addInfos(name, author, source, title, description, zIndex, sourceWidth, sourceHeigth, mini, thumb);
		try {
			// connection.commit();
			connection.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// add source, author,date , thumb
	}

	public void addInfos(String name, String author, String source, String title, String description, int zindex, int width, int height, byte[] mini, byte[] thumb) {
		// TABLE infos (name STRING, title STRING,description STRING, author
		// STRING, source STRING, date STRING, zindex INTEGER, miniature
		// BLOB,thumb BLOB)");

		// String stat = "INSERT INTO infos VALUES(\"" + name + "\",\"" + title
		// + "\",\""
		// + description + "\",\"" + author + "\",\"" + source + "\",\""
		// + System.currentTimeMillis() + "\"," + zindex + ", ?,?)";
		//		
		long mapID=0;
		//CREATE TABLE infos (title STRING, mapKey LONG, description STRING, author STRING, source STRING, date STRING, zindex LONG, " + "width LONG, height LONG," + "miniature BLOB,thumb BLOB)");
		
		String stat = "INSERT INTO infos VALUES(?,?,?,?,?,?,?,?,?,?,?)";
		String date = new Date(System.currentTimeMillis()).toString();
		System.out.println("stat = " + stat);
		try {
			PreparedStatement ps = connection.prepareStatement(stat);
			int i = 1;
			ps.setString(i++, title);
			ps.setLong(i++, mapKey);
//			ps.setString(i++, name);
			ps.setString(i++, description);
			ps.setString(i++, author);
			ps.setString(i++, source);
			ps.setString(i++, date);
			ps.setLong(i++, zindex);
			ps.setLong(i++, width);
			ps.setLong(i++, height);
			ps.setBytes(i++, mini);
			ps.setBytes(i++, thumb);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Information insertion failed.");
			e.printStackTrace();
		}
	}

	public void addTile(int x, int y, int z, byte[] data, String fileSansDot) {
		// String stat = "insert into ? values(" + x + "," + y + "," + z +
		// ",?)";
		try {
			insertTile.setInt(1, x);
			insertTile.setInt(2, y);
			insertTile.setInt(3, z);

			insertTile.setBytes(4, data);
			insertTile.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Export failed !");
			e.printStackTrace();
		}
	}

	public void addLevelInfos(String name, long mapID, int zoom, int width, int height, int tiles_x, int tiles_y, int offsetX, int offsetY) {

		String layerName="no name";
		long zindex = 0;
		String stat = "INSERT INTO layers_infos VALUES(\""+layerName+"\"," + mapID + ","+zindex+"," + zoom + "," + width + "," + height + "," + tiles_x + "," + tiles_y + "," + offsetX + "," + offsetY + ")";
		System.out.println("stat = " + stat);
		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate(stat);
		} catch (SQLException e) {
			System.err.println("Information insertion failed.");
			e.printStackTrace();
		}
	}

	public void calculateTiles(String destinationFile, String pathToFile, int tileSize, String tileType) throws IOException {
		System.out.println("calculating tiles...");
		long mapID = 0;

		if (destinationFile == null || pathToFile == null) { return; }
		// the pathTo file includes the fileName.
		File originalFile = new File(pathToFile);
		String fileSansDot = pathToFile.substring(pathToFile.lastIndexOf(File.separator) + 1, pathToFile.lastIndexOf("."));
		initSource(destinationFile, fileSansDot);

		// /////////////////////////////////
		System.out.println("creating the tiles");
		// //////////////////////////////
		ImageInputStream inStream = ImageIO.createImageInputStream(originalFile);
		BufferedImage img = ImageIO.read(inStream);
		// //////////////////////////////

		int width = img.getWidth();
		int height = img.getHeight();

		sourceWidth = width;
		sourceHeigth = height;

		int nbX = (width / tileSize) + 1;
		int nbY = (height / tileSize) + 1;

		int scaledWidth = width;
		int scaledHeight = height;
		int zoom = 0;
		// //////////////////////
		Dimension pictureTiles = new Dimension(nbX, nbY);
		Dimension pictureSize = new Dimension(width, height);
		// /////////
		// TODO : replace 500 by some variable/constant.
		double scaleX = MINIATURE_SIZE / (double) width;
		double scaleY = MINIATURE_SIZE / (double) height;
		double scaleFactor = Math.min(scaleX, scaleY);
		System.out.println("scaleX=" + scaleX + " scaleY=" + scaleY + " scale factor = " + scaleFactor);

		scaledWidth = (int) (width * scaleFactor);
		scaledHeight = (int) (height * scaleFactor);
		// TODO too complex : find some simpler way
		Image xxx = null;// miniature : img.getScaledInstance(scaledWidth,
		// scaledHeight, Image.SCALE_SMOOTH);
		// ImageIcon ic = new ImageIcon(xxx);
		BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaled.getGraphics();
		g.drawImage(xxx, 0, 0, null);
		g.dispose();
		// ////////////////////////////////////////////
		ByteArrayOutputStream byteStorage = new ByteArrayOutputStream();

		// Connection con = initSource(destinationFile, fileSansDot);
		byteStorage.reset();
		// ImageIO.write(scaled, tileType, byteStorage);

		name = fileSansDot;
		title = (null == title ? fileSansDot : title);
		description = (null == description ? "No Description" : description);

		addLevelInfos(fileSansDot, mapID, 0, width, height, nbX, nbY, 0, 0);
		// //////////////////////////
		// Creating Tiles.
		BufferedImage buffer = null;
		scaledWidth = width;
		scaledHeight = height;

		while (Math.min(scaledWidth, scaledHeight) > tileSize) {
			// localPath = currentDirPath + "/" + LAYER_PREFIX + zoom;
			// out.createLayer(localPath);
			// ///////////////////////////////////////////////
			// Writing the layer dimensions
			// dimensionFileName = scaledWidth + DIMENSION_SEPARATOR +
			// scaledHeight + LAYER_DIMENSION_SUFFIX;
			// out.createFile(localPath, dimensionFileName, new byte[] {});
			int fillX = 0;
			int fillY = 0;
			fillX = ((nbX * tileSize) - scaledWidth) / 2;
			fillY = ((nbY * tileSize) - scaledHeight) / 2;
			System.out.println("fill x =" + fillX + " fill y=" + fillY);
			for (int y = 0; y < nbY; y++) {
				for (int x = 0; x < nbX; x++) {
					int copyX = x * tileSize - fillX;
					int copyY = y * tileSize - fillY;
					int copyXX = tileSize;
					int copyYY = tileSize;
					int pasteXX = tileSize;
					int pasteYY = tileSize;
					int pasteX = 0;
					int pasteY = 0;

					// first column
					if (x == 0) {
						copyX = 0;
						copyXX = tileSize - fillX;
						pasteX = fillX;
						pasteXX = tileSize;
					}
					// first line
					if (y == 0) {
						copyY = 0;
						copyYY = tileSize - fillY;
						pasteY = fillY;
						pasteYY = tileSize;
					}
					// last column
					if (x == nbX - 1) {
						copyX = x * tileSize - fillX;
						copyXX = tileSize - fillX;
						pasteX = 0;
						pasteXX = copyXX;
					}

					// last line
					if (y == nbY - 1) {
						copyY = y * tileSize - fillY;
						copyYY = tileSize - fillY;
						pasteY = 0;
						pasteYY = copyYY;
					}
					// System.out.println("x ="+x+" y="+y+ " copyX="+
					// copyX+" copyY="+copyY+" copyWidth="+copyXX+" copyHeight="
					// +copyYY+ " pasteX=" + pasteX
					// +" -> pasteY="+pasteY+" pasteWidth="+
					// pasteXX+" pasteHeight=" +pasteYY );
					buffer = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
					Graphics2D g2 = buffer.createGraphics();
					g2.setColor(Color.DARK_GRAY);
					g2.fillRect(0, 0, tileSize, tileSize);

					g2.drawImage(img, pasteX, pasteY, pasteXX, pasteYY, copyX, copyY, copyX + copyXX, copyY + copyYY, null);
					g2.dispose();

					// //////////////////////////////////////
					// Writing the tiles
					try {
						byteStorage.reset();
						ImageIO.write(buffer, tileType, byteStorage);
						addTile(x, (nbY - 1) - y, zoom, byteStorage.toByteArray(), fileSansDot);
						// out.createImageFile(localPath, fileName, buffer,
						// tileType);// "jpg");
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					// //////////////////////////////////////
					// TODO NOTE : to save memory, re read everything
					// cleanly afterwards ?
					// ServerSideTile t = new ServerSideTile(x, y, 0, out);
					// tiles.put(getKey(x, y, 0), t);
				}
			}
			scaledWidth = (int) (scaledWidth * ZOOM_FACTOR);
			scaledHeight = (int) (scaledHeight * ZOOM_FACTOR);
			System.out.println("scaled width " + scaledWidth + " height " + scaledHeight);
			nbX = (scaledWidth / tileSize) + 1;
			nbY = (scaledHeight / tileSize) + 1;

			zoom++;

			xxx = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
			img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
			Graphics g0 = img.createGraphics();

			g0.drawImage(xxx, 0, 0, scaledWidth, scaledHeight, 0, 0, scaledWidth, scaledHeight, null);
			g0.dispose();
			System.out.println("zoom layer : " + zoom + " image size:" + img.getWidth() + "x" + img.getHeight());
			addLevelInfos(fileSansDot, mapID, zoom, scaledWidth, scaledHeight, nbX, nbY, 0, 0);
		}

		// //////////////////////////////////////
		int dim = Math.max(scaledHeight, scaledWidth);
		double factor = 500.0 / (double) dim;

		scaledWidth = (int) (scaledWidth * factor);
		scaledHeight = (int) (scaledHeight * factor);
		System.out.println("mini size : " + scaledWidth + " by " + scaledHeight);

		xxx = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
		img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g0 = img.createGraphics();

		g0.drawImage(xxx, 0, 0, scaledWidth, scaledHeight, 0, 0, scaledWidth, scaledHeight, null);
		g0.dispose();
		try {
			byteStorage.reset();
			ImageIO.write(img, tileType, byteStorage);
			mini = byteStorage.toByteArray();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		// //////////////

		dim = Math.max(scaledHeight, scaledWidth);
		factor = 48 / (double) dim;

		scaledWidth = (int) (scaledWidth * factor);
		scaledHeight = (int) (scaledHeight * factor);

		xxx = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
		System.out.println("thumb size : " + scaledWidth + " by " + scaledHeight);
		img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		g0 = img.createGraphics();
		g0.drawImage(xxx, 0, 0, scaledWidth, scaledHeight, 0, 0, scaledWidth, scaledHeight, null);
		g0.dispose();

		try {
			byteStorage.reset();
			ImageIO.write(img, tileType, byteStorage);
			thumb = byteStorage.toByteArray();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		// //////////////

		System.out.println("archive created");
		doneCalculating = true;
	}

	// public static void main(String[] args) throws Exception {
	//
	// String dest = "/Users/niko/tileSources/globcover_MOSAIC_H.db";
	// String src = "/Users/niko/tileSources/globcover_MOSAIC_H.png";
	// SQliteTileCreator creator = new SQliteTileCreator();
	// creator.calculateTiles(dest, src, 192, "png");
	// creator.finalizeFile();
	//
	// }

}