/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2017 - Joren Six / IPEM                             *
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      *
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     *
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    *
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      *
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/

package be.panako.tap;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class SerialDevice {

	private final SerialPort serialPort;
	private final SerialDataLineHandler handler;
	
	public SerialDevice(String portName,SerialDataLineHandler handler){
		serialPort = new SerialPort(portName);
		this.handler = handler;
	}
	
	public void open(){
		try {
            serialPort.openPort();//Open port
            serialPort.setParams(921600, 8, 1, 0);//Set params
            int mask = SerialPort.MASK_RXCHAR;//Prepare mask
        	//Set mask
			serialPort.setEventsMask(mask);
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }
	}
	

	
	public void start(){
		//Add SerialPortEventListener
		try {
			serialPort.addEventListener(new SerialPortLineReader(serialPort,handler));
		} catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stop(){
		try {
            serialPort.closePort();//close port
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }
	}
	
	public static interface SerialDataLineHandler{
		
		void handleSerialDataLine(int lineNumber, String lineData);
	}
	
	public void write(String line){
		if(!line.endsWith("\n")){
			line = line + "\n";
		}
		try {
			serialPort.writeString(line);
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
    
    /*
     * In this class must implement the method serialEvent, through it we learn about 
     * events that happened to our port. But we will not report on all events but only 
     * those that we put in the mask. In this case the arrival of the data and change the 
     * status lines CTS and DSR
     */
	public static class SerialPortLineReader implements SerialPortEventListener {
		private final SerialPort serialPort;
		private final StringBuilder message = new StringBuilder();
		private int lineNumber = 0;
		private final SerialDataLineHandler handler;

		public SerialPortLineReader(SerialPort serialPort, SerialDataLineHandler handler) {
			this.serialPort = serialPort;
			this.handler = handler;
		}

		public void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR() && event.getEventValue() > 0) {// If data is
																// available
				// Read data, if 10 bytes available
				byte buffer[] = null;
				try {
					buffer = serialPort.readBytes();
				} catch (SerialPortException ex) {
					System.out.println(ex);
				}
				for (byte b : buffer) {
					if (b == '\n') {
						lineNumber++;
						String lineData = message.toString();
						if (handler != null) {
							handler.handleSerialDataLine(lineNumber, lineData);
						}
						message.setLength(0);
					} else {
						message.append((char) b);
					}
				}
			}
		}
	}
    
    public static String[] getSerialPorts(){
    	return SerialPortList.getPortNames();
    }
    
    private static boolean measureLatency=  false;
     static SerialDevice spr = null;
    public static void main(String...strings){
    	spr = new SerialDevice("/dev/ttyACM0",new SerialDataLineHandler() {
			@Override
			public void handleSerialDataLine(int lineNumber, String lineData) {
				System.out.println(lineNumber + " " + lineData);
				if(measureLatency){
					spr.write("");
					measureLatency = false;
				}
			}
		});
    	spr.open();
    	spr.start();
    	
    	while(true){
    		try {
				Thread.sleep(5000);
				measureLatency=true;
				spr.write("");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
}
