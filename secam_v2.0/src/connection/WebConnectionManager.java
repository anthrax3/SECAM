package connection;

import java.io.IOException;
import java.net.InetAddress;

import javax.media.CannotRealizeException;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoDataSinkException;
import javax.media.NoProcessorException;
import javax.media.NotRealizedError;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.SourceCloneable;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.rtcp.SourceDescription;

/**
 * Web Connection Manager. Deals with connecting and streaming video data to the web applet.
 * @author Caleb Shortt, TJ Borschneck
 */

public class WebConnectionManager {
	
	/**
	 * This processor converts the input stream into a transmittable format for a network.
	 */
	private Processor networkProcessor = null;
	
	/**
	 * Is used to manage the transmitting network stream
	 */
	private RTPManager rtpManager = null;
	
	/**
	 * Output stream that transmits the video data over a network
	 */
	private SendStream stream = null;
	
	// -------------------------------------------------------------------------------
	private WebConnectionManager() {}
	// -------------------------------------------------------------------------------
	
	private static WebConnectionManager manager = null;
	
	/**
	 * Returns the instance of the WebConnectionManager singleton
	 * @return The connection manager singleton instance
	 */
	public static WebConnectionManager getInstance() {
		if(manager == null) {
			manager = new WebConnectionManager();
		}
		return manager;
	}
	
	/**
	 * Initializes the network broadcasting processors and data sinks. It does NOT start or stop them: there are separate 
	 * methods for that.
	 * 
	 * Starts the streaming to the web applet
	 * 
	 * @throws NoProcessorException
	 * @throws CannotRealizeException
	 * @throws IOException
	 * @throws NoDataSinkException
	 * @throws NotRealizedError
	 */
	public void initBroadcast(DataSource inputDataSource) throws NoProcessorException, CannotRealizeException, IOException, NoDataSinkException, NotRealizedError {
		
		DataSource source = ((SourceCloneable)inputDataSource).createClone();
		Format[] formats = new Format[] { new VideoFormat(VideoFormat.JPEG_RTP) };
		ProcessorModel model = new ProcessorModel(source, formats, new ContentDescriptor(ContentDescriptor.RAW_RTP));
		
		// TODO
		//System.err.println("\t\tCreating Network Processor");				// DEBUG
		
		networkProcessor = Manager.createRealizedProcessor(model);
		
		//System.err.println("\t\tGetting new instance of RTPManager");
		
		rtpManager = RTPManager.newInstance();
		SessionAddress local, dest;
		InetAddress ipAddr;
		
		int port = 22222;
		SourceDescription desc;
		
		try
		{
			ipAddr = InetAddress.getLocalHost();
			local = new SessionAddress();
			dest = new SessionAddress(ipAddr, port);
			
			// TODO
			//System.err.println("\t\tInitializing RTPManager to local (" + local + ")");
			
			rtpManager.initialize(local);
			
			// TODO
			//System.err.println("\t\tSetting TRPManager target to dest (" + dest + ")");
			
			rtpManager.addTarget(dest);
			
			// TODO
			//System.err.println("\t\tCreating Send Stream from Network Processor: " + networkProcessor.getDataOutput());
			
			stream = rtpManager.createSendStream(networkProcessor.getDataOutput(), 0);
			
			// TODO
			//System.err.println("\t\tStream Created: " + stream);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Starts the broadcast of the stream across the network
	 * @throws IOException
	 */
	public void startBroadcast() throws IOException
	{
		//System.out.println("\t--- Starting Network Broadcast ---");
		//System.err.println("\t\tStarting Network Processor: " + networkProcessor);
		networkProcessor.start();
		//System.err.println("\t\tStarting Stream: " + stream);
		stream.start();
		//System.out.println("\t--- Network Broadcast Started ---");
	}
	
	
	
	/**
	 * Stops the broadcast of the stream across the network
	 * @throws IOException
	 */
	public void stopBroadcast() throws IOException
	{
		//System.out.println("\t--- Stopping Network Broadcast ---");
		//System.err.println("\t\tStopping Network Processor: " + networkProcessor);
		networkProcessor.stop();
		//System.err.println("\t\tStopping Stream: " + stream);
		stream.stop();
	}
	
	
	
	
}
