package connection;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.VideoFormat;


/**
 * Manages the connection of the client to the database. 
 * @author Caleb Shortt
 */
public class ConnectionManager
{

	private Connection conn = null;
	private int streamID = -1;
	private int frameID = -1;
	private String currentPath = "";
	private boolean connected = false;

	// Path to database storage (Folder stores movies while database stores path
	// to file)
	public static final String dbPath = "DBStore" + File.separator;

	private FileOutputStream fOut = null;
	private VideoFormat vFormat = null;
	
	private String username = "root";
	private String userpass = "mynameis";
	
	private int auditCursor = 0;

	// -------------------------------------------------------------------------------
	public ConnectionManager()
	{
		//connect();
		//connected = true;
	}

	// -------------------------------------------------------------------------------

	
	/**
	 * Opens the connection to the specified database.
	 * @param
	 * @return
	 */
	public void connect()
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String url = "jdbc:mysql://localhost/secam_secure";
			conn = DriverManager.getConnection(url, username, userpass);
			connected = true;
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Safely closes the connection to the database and returns a boolean of success
	 * @param
	 * @return success (boolean)
	 */
	public boolean disconnect()
	{
		boolean success = false;
		try
		{
			conn.close();
			success = true;
			connected = false;
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return (success);
	}

	/**
	 * This simply creates a new entry in the database for a new stream. 
	 * It DOES NOT start the streaming. Returns the ID of the stream
	 * @param Information about the stream (StreamInfo)
	 * @return The ID of the newly created database entry. <b>Returns -1 if failed.</b>
	 */
	public int newStream(StreamInfo info)
	{

		streamID++;

		try
		{
			Statement s = conn.createStatement();
			Format format = info.getFormat();

			if (format instanceof VideoFormat)
			{
				vFormat = (VideoFormat) format;

				currentPath = "stream" + streamID + ".mpg";

				String query = "INSERT INTO videos VALUES (";
				query += streamID + "," + info.getUserID() + "," + info.getCompID() + ",";
				query += "'" + info.getStartDate() + "','" + info.getEndDate() + "','" + currentPath + "')";

				s.execute(query);

				fOut = new FileOutputStream(new File(dbPath + currentPath), true);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return streamID;
	}
	
	
	/**
	 * Creates a record in the database for the new video and returns the path to it. It does NOT 
	 * create a file to store the video.
	 * @param info
	 * @return
	 */
	public String createStream(StreamInfo info)
	{
		streamID++;
		File outfile = null;

		try
		{
			Statement s = conn.createStatement();
			Format format = info.getFormat();

			if (format instanceof VideoFormat)
			{
				vFormat = (VideoFormat) format;

				currentPath = "stream" + streamID + ".mov";

				String query = "INSERT INTO videos VALUES (";
				query += streamID + "," + info.getUserID() + "," + info.getCompID() + ",";
				query += "'" + info.getStartDate() + "','" + info.getEndDate() + "','" + currentPath + "')";

				s.execute(query);
				
				outfile = new File(dbPath + currentPath);
				//fOut = new FileOutputStream(outfile, true);
				//fOut.close();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		
		return (outfile.getAbsolutePath());
	}
	
	
	/**
	 * Update the given stream with the given buffer.
	 * @param <b>stream ID</b> (int)
	 * @param buffer (Buffer)
	 * @return void
	 */
	public void updateStream(int id, Buffer inBuffer)
	{
		try
		{
			//Statement s = conn.createStatement();

			// dbPath
			if (id == streamID)
			{
				if (inBuffer.getData() != null)
				{
					byte[] data = (byte[]) inBuffer.getData();
					BufferedOutputStream out = new BufferedOutputStream(fOut);
					out.write(data);
				}
			}

		} catch (IOException ioe)
		{
			System.err.println("An IO Exception has occurred.");
			ioe.printStackTrace();
		} catch (Exception e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	
	/**
	 * Verify the login information that is provided. 
	 * Adds the login attempt to the audit_login table.
	 * Returns if the login is valid or not.
	 * @param username
	 * @param password
	 * @return
	 */
	public boolean attemptLogin(String user, String pass, String name)
	{
		boolean success = false;
		
		// Attempt to retrieve the user object from the user table
		// compare user names and passwords
		// if they match, success = true
		// else false
		
		// Add data to the audit_login table
		// return success
		
		if(!connected)
			return success;
		
		user = scrubString(user);
		pass = scrubString(pass);
		name = scrubString(name);
		
		ResultSet result = null;
		String dbUser = "";
		String dbPass = "";
		int dbUserID  = -1;
		
		// Attempt Login
		try
		{
			Statement s = conn.createStatement();
			String query = "SELECT * FROM users WHERE username = '" + user + "' AND password = '" + pass + "'";
			
			result = s.executeQuery(query);
			
			
			if(result.next())
			{
				dbUser = result.getString("username");
				dbPass = result.getString("password");
				dbUserID = result.getInt("id");
			}
			if(!user.isEmpty() && !pass.isEmpty() && dbUser.equals(user) && dbPass.equals(pass))
				success = true;
			
		}
		catch (Exception e)
		{
			System.err.println("Failed database query attempt.");
		}
		
		// Log the login attempt
		try {
			Statement s = conn.createStatement();
			String query = "INSERT INTO audit_login VALUES (" + (auditCursor++) + ",'" + user + "'," + dbUserID + ",0," + ((success) ? "'Y'" : "'N'") + ",now())";
			s.execute(query);
		}
		catch (Exception e)
		{
			System.err.println("[Critical Error] An error occurred while logging.");
		}
		
		return success;
	}
	
	
	/**
	 * Try to scrub any SQL from the string (Very basic)
	 * @param str
	 * @return
	 */
	private String scrubString(String str)
	{
		return str.replaceAll("[;\\\"\n\r=//*\'<>{}]", "");
	}
	
	
	/**
	 * Returns if connected to the database.
	 * @param
	 * @return connected (boolean)
	 */
	public boolean isConnected()
	{
		return connected;
	}
	
	 
	public void setAuditCursor(int cursor)
	{
		this.auditCursor = cursor+1;
	}
	
	public int getAuditCursor()
	{
		return this.auditCursor;
	}
	
	public void setStreamCursor(int cursor)
	{
		this.streamID = cursor+1;
	}
	
	public int getStreamCursor()
	{
		return this.streamID;
	}
	
	
	/**
	 * This main function is purely for debugging purposes. It 
	 * runs a series of tests that are contained within this class to 
	 * validate the database queries.
	 * @param Input argument string array
	 * @return void
	 */
	public static void main(String[] args)
	{
		ConnectionManager m = new ConnectionManager();
		m.connect();
		
		m.doTest();
		
		// StreamInfo info = new
		// StreamInfo(0,0,"2010-05-06 11:50:20","2010-05-06 11:51:21"); //
		// YYYY-MM-DD HH:mm:SS
		// m.newStream(info);
		
		System.out.println("\n\nStart Login Test");
		System.out.println( "Login Success Test Passed: " + m.attemptLogin("cshortt", "password", "caleb shortt") );
		System.out.println( "Login Failure Test Passed: " + !m.attemptLogin("cshortt", "notpassword", "caleb shortt") );
		System.out.println("End Login Test");
		
		m.disconnect();
	}

	// -------------------------------------------------------------------------------
	// TESTS
	// -------------------------------------------------------------------------------

	
	/**
	 * The main test function. It calls all of the other test functions in order to validate 
	 * the database queries.
	 * @param
	 * @return void
	 */
	public void doTest()
	{

		System.out.println("[START TEST]\n");

		SelectTest();
		insertTest();
		SelectTest();
		updateTest();
		SelectTest();
		deleteTest();
		SelectTest();

		System.out.println("\n[END TEST]");
	}

	
	/**
	 * Runs a basic test on the users table - gets the id, first and last name
	 * @param
	 * @return void
	 */
	private void SelectTest()
	{
		System.out.println("[Select Output]");
		String query = "SELECT id, first_name, last_name FROM users";

		try
		{
			Statement s = conn.createStatement();
			ResultSet result = s.executeQuery(query);

			while (result.next())
			{
				String id = result.getString("id");
				String first_name = result.getString("first_name");
				String last_name = result.getString("last_name");
				System.out.println(id + ": " + first_name + " " + last_name);
			}

		} catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	
	/**
	 * Inserts a single record into that users table - cannot execute twice as 
	 * the id is the same.
	 * @param
	 * @return void
	 */
	private void insertTest()
	{
		System.out.println("[Performing Insertion]");
		try
		{
			Statement s = conn.createStatement();
			s.execute("INSERT INTO users VALUES (999,'Caleb','Shortt','','cshortt','mypassword')");

		} catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Changes the id of the created record from 999 to 1000
	 * @param
	 * @return void
	 */
	private void updateTest()
	{
		System.out.println("[Performing Update]");
		try
		{
			Statement s = conn.createStatement();
			s.execute("UPDATE users SET id=1000 WHERE id=999");

		} catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	
	/**
	 * Deletes the record with id 1000
	 * @param
	 * @return void
	 */
	private void deleteTest()
	{
		System.out.println("[Performing Deletion]");
		try
		{
			Statement s = conn.createStatement();
			s.execute("DELETE FROM users WHERE id=1000");

		} catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

}
