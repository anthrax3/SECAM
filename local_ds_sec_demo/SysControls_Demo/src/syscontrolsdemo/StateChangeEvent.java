package syscontrolsdemo;

import java.util.EventObject;

/**
 * The event for signalling that the system state has changed (activated or deactivated)
 * @author Caleb Shortt
 */
public class StateChangeEvent extends EventObject
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StateChangeEvent(Object source)
	{
		super(source);
	}

}
