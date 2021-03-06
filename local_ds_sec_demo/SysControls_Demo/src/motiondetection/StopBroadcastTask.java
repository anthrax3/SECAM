package motiondetection;

import java.util.TimerTask;

import javax.media.DataSink;
import javax.media.Processor;

import syscontrolsdemo.SystemState;

import connection.ConnectionManager;

public class StopBroadcastTask extends TimerTask
{
	private ConnectionManager manager = null;
	private DataSink broadcaster = null;
	private Processor streamProcessor = null;
	
	public StopBroadcastTask(ConnectionManager manager, DataSink broadcaster, Processor streamProcessor)
	{
		this.manager = manager;
		this.broadcaster = broadcaster;
		this.streamProcessor = streamProcessor;
	}
	
	public void run()
	{
		try {
			System.out.println("BROADCAST HAS BEEN TERMINATED");
			broadcaster.stop();
			broadcaster.close();
			streamProcessor.stop();
			//manager.disconnect();
			SystemState.Recording = false;
		} 
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
