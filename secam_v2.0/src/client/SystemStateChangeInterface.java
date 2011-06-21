package client;

import java.util.EventListener;

import client.StateChangeEvent;


/**
 * A simple interface that every class must implement if it wants to listen to the System State changes.
 * @author Caleb Shortt
 */
public interface SystemStateChangeInterface extends EventListener
{
	public void stateChanged(StateChangeEvent sce);
}
