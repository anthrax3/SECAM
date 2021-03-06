
package syscontrolsdemo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import connection.ConnSingleton;


/**
 * Allows the user to connect to the SECAM System. Requires a login.
 * @author Caleb Shortt
 */
public class ConnectPane {

	
	private Display display = null;
	private Shell shell = null;
	
	private Label status = null;
	
	private Button connect = null;
	private Button cancel = null;
	
	private Label l_user = null;
	private Label l_pass = null;
	private Label l_name = null;
	
	private Text t_user = null;
	private Text t_pass = null;
	private Text t_name = null;
	
	final int MAX_WIDTH = 250;
	final int MAX_HEIGHT = 150;
	
	private String name;
	private String user;
	private String pass;
	
	
	/**
	 * Initialises and runs the GUI
	 */
	public ConnectPane(Display d) {
		display = d;
		init();
		run();
	}
	
	
	/**
	 * Initialises the GUI; this includes element layout and listeners.
	 * @param
	 * @return void
	 */
	private void init() {
		//display = new Display();
		shell = new Shell(display);
		
		name = "";
		user = "";
		pass = "";
		
		GridLayout g_layout = new GridLayout(2, true);
		g_layout.marginWidth = 15;
		g_layout.marginHeight = 15;
		shell.setLayout(g_layout);
		
		//l_name = new Label(shell, SWT.FILL);
		//l_name.setText("Name (To be displayed): ");
		//t_name = new Text(shell, SWT.BORDER);
		
		
		l_user = new Label(shell, SWT.FILL);
		l_user.setText("User Name: ");
		t_user = new Text(shell, SWT.BORDER);
		
		l_pass = new Label(shell, SWT.FILL);
		l_pass.setText("Password: ");
		t_pass = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		
		connect = new Button(shell, SWT.PUSH);
		connect.setText("Connect");
		
		cancel = new Button(shell, SWT.PUSH);
		cancel.setText("Cancel");
		
		status = new Label(shell, SWT.FILL);
		status.setText("");
		
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				shell.dispose();
			}
		});
		
		connect.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				
				// Grab data from text fields
				//name = t_name.getText();
				user = t_user.getText();
				pass = t_pass.getText();
				status.setText("Attempting to login");
				
				if(ConnSingleton.getInstance().attemptLogin(getUser(), getPassword(), getName()))
				{
					SystemState.LoggedIn = true;
					shell.dispose();
				}
				else
				{
					status.setText("Invalid Login");
					display.update();
					SystemState.LoggedIn = false;
				}
				
			}
		});
	}
	
	
	/**
	 * Runs the GUI once it has been initialised.
	 * @return void
	 */
	public void run() {
		shell.setSize(MAX_WIDTH,MAX_HEIGHT);
		shell.setMinimumSize(MAX_WIDTH, MAX_HEIGHT);
		shell.setText("Connect to the SECAM System");
		shell.open();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		//display.dispose();
		shell.dispose();
	}
	
	
	/*******************************************************************************************
	 * 							Start Utility Functions
	 *******************************************************************************************/
	public String getName()
	{
		return name;
	}
	
	public String getPassword()
	{
		return pass;
	}
	
	public String getUser()
	{
		return user;
	}
	/*******************************************************************************************
	 * 							End Utility Functions
	 *******************************************************************************************/
	
	
	/**
	 * Initialises the ConnectPane class and runs it.
	 * @param Input
	 */
	public static void main(String[] args) {
		new ConnectPane(new Display());
		System.exit(0);
	}
}
