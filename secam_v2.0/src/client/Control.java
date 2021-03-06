package client;

import java.awt.Frame;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

import javax.media.DataSink;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.SourceCloneable;

import connection.DatabaseConnectionManager;
import connection.StreamInfo;
import connection.WebConnectionManager;
import motiondetection.MotionDetector;
import motiondetection.MotionEvent;
import motiondetection.MotionListener;
import motiondetection.MotionListenerInterface;
import motiondetection.StopBroadcastTask;

public class Control implements MotionListenerInterface, SystemStateChangeInterface {
	
	private MotionDetector motionDetector = null;
	private DatabaseConnectionManager dbConnManager = null;
	private WebConnectionManager webConnManager = null;
	private File filePointer = null;
	private File currentFile = null;
	
	/**
	 * Number of <b>minutes</b> that the system will record the video stream when motion is detected.
	 */
	private double recordingInterval = 0.5; // record for .5 of a minute; or 30 seconds.
	
	/**
	 * This is the processor that will take the webcam datasource and and convert it into the specified format for
	 * storage (save to file)
	 */
	private Processor fileStreamProcessor = null;
	
	/**
	 * This media locator will specify the location that the stream will be saved to (saved to file)
	 */
	private MediaLocator fileDestinationLocator = null;
	
	/**
	 * This is the data sink that will broadcast the stream to the file specified by fileDestinationLocator
	 */
	private DataSink fileBroadcaster = null;
	
	private SimpleDateFormat formatter = null;
	private Date currentTime = null;
	private Date targetTime = null;
	
	
	/**
	 * Constructor: Takes a frame that will display the results from the motion detection, and 
	 * also takes the location of the media to be used (Camera feed)
	 * @param frame
	 */
	public Control(MediaLocator ds, Frame frame) {
		MotionListener.addMotionListener(this);
		dbConnManager = DatabaseConnectionManager.getInstance();
		webConnManager = WebConnectionManager.getInstance();
		motionDetector = new MotionDetector(ds, frame);
		formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	}
	
	
	/**
	 * Attempts to initiate the file streaming broadcast (save to file) - on success, initiates the 
	 * web stream broadcast.
	 */
	public void initBroadcast() {
		
		if (initFileBroadcast()) {
			try {
				webConnManager.initBroadcast(motionDetector.getInputDataSource());
				
			} catch (Exception e) {
				System.err.println("[Error] Could not initiate web broadcast: " + e);
			}
		}
	}
	
	
	/**
	 * Attempts to initiate the variables for file broadcast. (Accesses database, etc)
	 * @return Returns if successful or not
	 */
	private boolean initFileBroadcast() {

		if(!dbConnManager.isConnected())
			dbConnManager.connect();
		
		// File type to save to file (Database)
		FileTypeDescriptor outputType = new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME);
		DataSource videoSource = ((SourceCloneable)motionDetector.getInputDataSource()).createClone();
		Format[] formats = { new VideoFormat(VideoFormat.CINEPAK) };
		
		// Create a stream info object with the stream information
		StreamInfo info = new StreamInfo(0, 0, formatter.format(new Date()), formatter.format(new Date()), formats[0]);
		currentFile = dbConnManager.createStream(info);
		
		
		
		// Initiate the processor, the locator, and the datasink that will be used to save the stream to file.
		try {
			
			// TODO
			System.out.println("[Processor] Will be using videosource: " + videoSource.toString());
			System.out.println("[Processor] Will be using format(a): " + formats.toString());
			System.out.println("[Processor] Will be using output type: " + outputType.toString());
			
			
			fileStreamProcessor = Manager.createRealizedProcessor(new ProcessorModel(videoSource, formats, outputType));
			fileStreamProcessor.configure();
			
			DataSource processorOutput = fileStreamProcessor.getDataOutput();
			fileDestinationLocator = new MediaLocator("file:" + File.separator + currentFile.getAbsolutePath());
			
			// TODO
			System.out.println("[Datasink] Will be saving the file to: " + fileDestinationLocator);
			System.out.println("[Datasink] Will be using the processorOutput: " + processorOutput.toString());
			
			fileBroadcaster = Manager.createDataSink(processorOutput, fileDestinationLocator);
			fileBroadcaster.open();
			
		} catch (Exception e) {
			System.err.println("[Error] Unable to initiate broadcast to file: " + e);
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	public void startBroadcast() {
		
		try {
			// Start streaming to file
			fileStreamProcessor.start();
			//fileBroadcaster.open();
			fileBroadcaster.start();
			
			// Start streaming to web
			webConnManager.startBroadcast();
			
			SystemState.Recording = true;
			
			// Set up a timer to stop the streaming after a certain recording interval
			Timer timer = new Timer();
			timer.schedule(new StopBroadcastTask(currentFile, fileStreamProcessor, fileBroadcaster, webConnManager), (long)(recordingInterval*60*1000));
		} catch (Exception e) {
			System.err.println("[ERROR] Could not start general broadcast: " + e);
			e.printStackTrace();
		}
	}
	
	
	public void stopBroadcast() {
		try {
			fileStreamProcessor.stop();
			
			String name = currentFile.getPath();
			name = name.replaceAll("temp.", "");
			name = name.replaceAll(".nonstreamable", "");
			currentFile.renameTo(new File(name));
			fileBroadcaster.stop();
			fileBroadcaster.close();
			webConnManager.stopBroadcast();
		} catch(Exception e) {
			System.err.println("[Warning]\tFailed to stop broadcast.");
		}
	}
	
	
	public void cleanup() {
		stopBroadcast();
		try {
			dbConnManager.disconnect();
		} catch(Exception e) {
			System.err.println("[Warning]\tCould not close database connection.");
		}
	}
	
	
	public Frame getFrame() {
		return motionDetector.getFrame();
	}
	

	/**
	 * The motion event handler for this class. When motion 
	 * is detected, this function is called.
	 * @param motionevent (MotionEvent)
	 * @return void
	 */
	public void motion(MotionEvent me) {
		if (!SystemState.Recording && SystemState.Activated)
		{
			initBroadcast();
			startBroadcast();
		}
	}


	/**
	 * Called when the system state changes. Handles the resulting logic.
	 * @param StateChangeEvent
	 */
	public void stateChanged(StateChangeEvent sce) {
		if(!SystemState.Activated)
		{
			stopBroadcast();
		}
	}
}
