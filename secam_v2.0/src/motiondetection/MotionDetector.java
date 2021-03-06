package motiondetection;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.media.Codec;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.UnsupportedPlugInException;
import javax.media.control.TrackControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;

public class MotionDetector implements ControllerListener
{
	
	private Frame frame = null;
	private SimpleDateFormat formatter = null;
	private boolean stateTransitionOK = true;
	private Object waitSync = new Object();
	
	/**
	 * Specifies the location of the camera to be used for motion detection
	 */
	private MediaLocator inputMediaLocator = null;
	/**
	 * This is the data source created from the inputMediaLocator variable 
	 */
	private DataSource inputDataSource = null;
	/**
	 * This is the processor that will display the stream to the client GUI screen
	 */
	private Processor displayProcessor = null;
	/**
	 * List of track controls from the display processor
	 */
	private TrackControl tc[];
	/**
	 * Selected video track from the list of track controls to be used
	 */
	private TrackControl videoTrack = null;
	
	
	
	
	/**
	 * The default constructor will create a new test Frame to be used 
	 * by the motion detector system.
	 */
	public MotionDetector()
	{
		frame = new Frame("Test Motion Detection");
	}
	
	
	/**
	 * This overrided constructor takes a data source and a frame. The 
	 * data source is the location to the video stream (Webcam) and the 
	 * provided frame will be used to display the output video stream. 
	 * <b>This is the main constructor of this class and should be 
	 * initialised for its use.</b>
	 * @param datasource (MediaLocator)
	 * @param frame (Frame)
	 */
	public MotionDetector(MediaLocator ds, Frame frame)
	{
		this.frame = frame;
		formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		inputMediaLocator = ds;

		if (!open(inputMediaLocator))
		{
			System.err.println("Could not create stream");
			System.exit(0);
		}
	}
	
	
	/**
	 * Given a datasource, create a processor and use that processor as a player
	 * to playback the media. During the processor's Configured state, the MotionDetectionEffect and
	 * TimeStampEffect is inserted into the video track.
	 * @param datasource (MediaLocator)
	 * @return success (boolean)
	 */
	public boolean open(MediaLocator ds)
	{

		try
		{
			DataSource source = Manager.createDataSource(ds);
			inputDataSource = Manager.createCloneableDataSource(source);
			displayProcessor = Manager.createProcessor(inputDataSource);
		} catch (Exception e)
		{
			System.err.println("Failed to create a processor from the given datasource: " + e);
			return false;
		}

		displayProcessor.addControllerListener(this);

		// Put the Processor into configured state.
		displayProcessor.configure();
		if (!waitForState(displayProcessor.Configured))
		{
			System.err.println("Failed to configure the processor.");
			return false;
		}

		// So I can use it as a player.
		displayProcessor.setContentDescriptor(null);
		// Obtain the track controls.
		tc = displayProcessor.getTrackControls();

		if (tc == null)
		{
			System.err.println("Failed to obtain track controls from the processor.");
			return false;
		}

		// Search for the track control for the video track.
		for (int i = 0; i < tc.length; i++)
		{
			if (tc[i].getFormat() instanceof VideoFormat)
			{
				videoTrack = tc[i];
				break;
			}
		}

		if (videoTrack == null)
		{
			System.err.println("The input media does not contain a video track.");
			return false;
		}

		// Instantiate and set the frame access codec to the data flow path.
		try
		{
			Codec codec[] = { new MotionDetectionEffect(), new TimeStampEffect() };
			videoTrack.setCodecChain(codec);
		} catch (UnsupportedPlugInException e)
		{
			System.err.println("The processor does not support effects.");
		}

		// Realize the processor.
		displayProcessor.prefetch();
		if (!waitForState(displayProcessor.Prefetched))
		{
			System.err.println("Failed to realize the processor.");
			return false;
		}
		// Display the visual & control component if there's one.

		// Get the player. Or construct the player from the processor

		frame.setLayout(new BorderLayout());

		// Component cc;

		Component vc;
		if ((vc = displayProcessor.getVisualComponent()) != null)
		{
			frame.add("Center", vc);
		}
		// Start the processor.
		displayProcessor.start();
		return true;
	}
	
	
	
	/**
	 * Block until the processor has transitioned to the given state. Return
	 * false if the transition failed.
	 * @param state (int)
	 * @return success (boolean)
	 */
	public boolean waitForState(int state)
	{
		synchronized (waitSync)
		{
			try
			{
				while (displayProcessor.getState() != state && stateTransitionOK)
					waitSync.wait();
			} 
			catch (Exception e)
			{
			}
		}
		return stateTransitionOK;
	}
	
	
	
	/**
	 * Controller Listener.
	 * @param event (ControllerEvent)
	 * @return void
	 */
	public void controllerUpdate(ControllerEvent evt)
	{
		if (evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent || evt instanceof PrefetchCompleteEvent)
		{
			synchronized (waitSync)
			{
				stateTransitionOK = true;
				waitSync.notifyAll();
			}
		} 
		else if (evt instanceof ResourceUnavailableEvent)
		{
			synchronized (waitSync)
			{
				stateTransitionOK = false;
				waitSync.notifyAll();
			}
		} 
		else if (evt instanceof EndOfMediaEvent)
		{
			displayProcessor.close();
			System.exit(0);
		}
	}
	
	
	
	/**
	 * Closes the processor and cleans up the system.
	 * @param none
	 * @return void
	 * @throws IOException 
	 */
	public void cleanup()
	{
		try {
			displayProcessor.stop();
			displayProcessor.close();
		} catch (Exception e) {
			System.err.println("[Warning]\tCould not clean up display processor.");
		}
	}
	
	
	
	/**
	 * @param none
	 * @return The display frame
	 */
	public Frame getFrame()
	{
		return frame;
	}
	
	
	/**
	 * 
	 */
	public DataSource getInputDataSource() {
		return inputDataSource;
	}
}
