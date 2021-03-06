package connection;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.media.format.VideoFormat;

import org.eclipse.swt.widgets.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DatabaseConnectionManagerTest {
	
	private DatabaseConnectionManager manager = null;
	
	
	@Before
	public void setUp() throws Exception {
		manager = DatabaseConnectionManager.getInstance();
		manager.connect();
	}

	@After
	public void tearDown() throws Exception {
		manager.disconnect();
		manager = null;
	}
	
	@Test
	public void attemptLogin() {
		boolean result = manager.attemptLogin("test", "testpass", "Testy McTesterson");
		assert result;
		result = manager.attemptLogin("", "", "");
		assert !result;
	}
	
	@Test
	public void newStream() {
		SimpleDateFormat formatter = new SimpleDateFormat();
		StreamInfo info = new StreamInfo(0, 0, formatter.format(new Date()), formatter.format(new Date()), new VideoFormat(VideoFormat.CINEPAK));
		int streamID = manager.newStream(info);
		boolean result = (streamID != -1);
		assert result;
	}
	
	@Test
	public void createStream() {
		SimpleDateFormat formatter = new SimpleDateFormat();
		StreamInfo info = new StreamInfo(0, 0, formatter.format(new Date()), formatter.format(new Date()), new VideoFormat(VideoFormat.CINEPAK));
		File path = manager.createStream(info);
		assert (path != null);
	}
}
