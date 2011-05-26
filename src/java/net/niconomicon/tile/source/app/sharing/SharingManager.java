/**
 * 
 */
package net.niconomicon.tile.source.app.sharing;

import java.util.Collection;

import net.niconomicon.tile.source.app.sharing.server.DisplayableSharingServiceAnnouncer;
import net.niconomicon.tile.source.app.sharing.server.jetty.JettyImageServerServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * @author niko
 * This class is designed to handle the activation of the Displayable server and its announcement.
 */
public class SharingManager {

	int port = -1;
	DisplayableSharingServiceAnnouncer sharingAnnouncer;
	JettyImageServerServlet service;
	Server server;
	ServletContextHandler context;

	public SharingManager() {
		sharingAnnouncer = sharingAnnouncer.getInstance();
		service = new JettyImageServerServlet();

		context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(new ServletHolder(service), "/*");

	}

	public void setPort(int port) throws Exception {
		this.port = port;
		sharingAnnouncer.setPort(port);
		if (null != server && server.isRunning()) {
			startSharing();
		}
	}

	public boolean isSharing() {
		if (null != server) { return server.isRunning(); }
		return false;
	}

	public void setSharingList(Collection<String> sharedMaps) {
		service.addImages(sharedMaps);
		//restartAnnouncer();
	}

	public void startSharing() throws Exception {
		System.out.println("supposedly starting to share on port :" + port);

		if (null != server && server.isRunning()) {
			server.stop();
		}
		server = new Server(port);
		server.setHandler(context);
		server.start();
		sharingAnnouncer.startSharing(port);
	}

	public void stopSharing() throws Exception {
		if (server != null) {
			server.stop();
		}
		if (sharingAnnouncer != null) {
			sharingAnnouncer.stopSharing();
		}
	}

	public void restartAnnouncer() {
		Thread t = new Thread(new Restarter());
		t.start();
	}

	private class Restarter implements Runnable {
		public void run() {
			sharingAnnouncer.stopSharing();
			sharingAnnouncer.reactivateSharing();
		}
	}
}
