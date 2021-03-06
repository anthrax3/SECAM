package connection;


/**
 * A manager for a ConnectionManager singleton.
 * @author Caleb Shortt
 *
 */
public class ConnSingleton {
	
	private static ConnectionManager manager = null;
	
	/**
	 * Returns the instance of the ConnectionManager singleton
	 * @return The connection manager singleton instance
	 */
	public static ConnectionManager getInstance() {
		if(manager == null) {
			manager = new ConnectionManager();
			manager.connect();
		}
		return manager;
	}
}
