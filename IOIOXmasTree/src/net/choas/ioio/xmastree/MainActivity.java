package net.choas.ioio.xmastree;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class MainActivity extends IOIOActivity {
	private ToggleButton recordButton;
	private ToggleButton[] buttons = new ToggleButton[7];
	protected boolean recording = false;
	
	private List<Record> records = new ArrayList<Record>();
	private int next = 0;
	
	private class Record {
		long time;
		int led;
		boolean status;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordButton = (ToggleButton) findViewById(R.id.button1);
        
        recordButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
				recording  = ((ToggleButton)v).isChecked();
				((ToggleButton)v).setText(recording ? "recording..." : "Record");
				if (recording) {
					records = new ArrayList<Record>();
					for (int i=0; i < 7; i++) {
						buttons[i].setChecked(false);
					}
				}
				next = 0;
			}
		});
        
        buttons[0] = (ToggleButton) findViewById(R.id.button7);
        buttons[1] = (ToggleButton) findViewById(R.id.button5);
        buttons[2] = (ToggleButton) findViewById(R.id.button3);
        buttons[3] = (ToggleButton) findViewById(R.id.button2);
        buttons[4] = (ToggleButton) findViewById(R.id.button4);
        buttons[5] = (ToggleButton) findViewById(R.id.button6);
        buttons[6] = (ToggleButton) findViewById(R.id.button8);

        for (int i = 0; i < 7; i++) {
        	buttons[i].setOnClickListener(new OnClickListener() {
				public void onClick(View v) {

					if (recording) {
						Record record = new Record();
						record.led = v.getId();
						record.status = ((ToggleButton)v).isChecked();
						record.time = (new Date()).getTime();
						records.add(record );
					}
					
				}
			});
        }
    }

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {
		private DigitalOutput[] leds = new DigitalOutput[7];
		private int[] pins = {5, 4, 3, 13, 12, 11, 10};
		boolean toggle = true;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			for (int i = 0; i < 7; i++) {
				leds[i] = ioio_.openDigitalOutput(pins[i], true);
			}
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException {
			long sleep = 2000;
			
			if (recording || records.size() == 0) {
				for (int i = 0; i < 7; i++) {
					leds[i].write(!toggle);
					toggle = toggle ? false : true;
				}
			} else {
				if (next == 0) {
					for (int i = 0; i < 7; i++) {
						leds[i].write(false);
					}
				}
				
				if (next < records.size()) {
					Record r = records.get(next);
					// search button
					int b=0;
					for (; b < 7; b++) {
						if (buttons[b].getId() == r.led) {
							break;
						}
					}
					leds[b].write(r.status);
					next++;
					if (next < records.size()) {
						Record r2 = records.get(next);
						sleep = r2.time - r.time;
						if (sleep > 1000) {
							sleep = 1000;
						}
					} else {
						sleep = 100;
					}
				}
				if (next >= records.size()) {
					next = 0;
					for (int i = 0; i < 7; i++) {
						leds[i].write(false);
					}
					sleep = 100;
				}
			}
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}
