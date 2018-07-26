package org.hyperthermia.serialTest;

/*
 * By José Manuel Terrés
 * Hyperthermia GUI project, 2018
 */

/******************************
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~*
 *~~~~~ Here Be Dragons ~~~~~~*
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~*
 ******************************/

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jssc.*;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLayeredPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JSpinner;
import javax.swing.UIManager;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpinnerListModel;
import java.awt.Font;
import javax.swing.JTextField;

public class SerialFrame implements SerialPortEventListener {

	/**
	 * Variable initializations
	 */
	private JFrame frmOpticalHyperthermiaGui;
	static SerialFrame actualFrame;
	public boolean connStatus = false;
	JComboBox<String> portList = new JComboBox<String>();
	JTextArea txtLog = new JTextArea();
	DefaultCaret caret = (DefaultCaret)txtLog.getCaret();
	JPanel camerapanel = new JPanel();
	VideoCapture webSource = null;
	JSpinner camNumber = new JSpinner();
	JLabel timeLeftLbl = new JLabel("00:00");
	JButton btnStop = new JButton("STOP");
	JButton btnStart = new JButton("START");
	JButton btnMove = new JButton("MOVE");
	JButton btnZero = new JButton("");
	private Timer cameraTimer = null;
	private Timer experimentTimer = null;
	boolean cameraIsStarted = false;
	int timeLeftMinutes = 0;
	int timeLeftSeconds = 0;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SerialFrame window = new SerialFrame();
					window.frmOpticalHyperthermiaGui.setVisible(true);
					actualFrame = window;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @wbp.parser.entryPoint
	 */
	public SerialFrame() {
		initialize();
	}
	
	// Read system available port names
	// (Call this from a button to refresh the list)
	public String[] readPortNames() {
		String[] portNames = null;
		portNames = SerialPortList.getPortNames();
		if (portNames.length == 0) {
			JOptionPane.showMessageDialog(null, "No COM ports found");
		}
		return portNames;
	}

	// Connect to a given port number
	SerialPort connectedPort;
	
	public boolean connectToPort(String selectedSerialPort) {
		SerialPort serialPort = new SerialPort(selectedSerialPort);
		try {
			serialPort.openPort();
			serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);//FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
			serialPort.addEventListener(actualFrame, SerialPort.MASK_RXCHAR);
			connectedPort = serialPort;
			return true;
		} catch (Exception e) {
			txtLog.append("Error opening port\r\n");
			return false;
		}
	}
	
	// Disconnect from any port GUI is connected to
	public boolean disconnectFromPort() {
		try {
			connectedPort.closePort();
			return false;
		} catch (Exception e) {
			System.out.println("There was a problem while disconnecting");
			return true;
		}
	}
	
	// Initialize GUI elements
	private void initialize() {
		frmOpticalHyperthermiaGui = new JFrame();
		frmOpticalHyperthermiaGui.setResizable(false);
		frmOpticalHyperthermiaGui.setTitle("Optical Hyperthermia GUI");
		frmOpticalHyperthermiaGui.setBounds(100, 100, 656, 481);
		frmOpticalHyperthermiaGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmOpticalHyperthermiaGui.getContentPane().setLayout(null);
		Image iconimg;
		
		JLayeredPane ConnPane = new JLayeredPane();
		ConnPane.setBounds(10, 11, 135, 103);
		
		
		ConnPane.setBorder(new TitledBorder(null, "Port Connections", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		frmOpticalHyperthermiaGui.getContentPane().add(ConnPane);
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.setBounds(8, 62, 121, 23);
		ConnPane.add(btnConnect);
		btnConnect.setBorder(BorderFactory.createLineBorder(Color.red));
		
		
		portList.setBounds(32, 31, 97, 20);
		ConnPane.add(portList);
		portList.setToolTipText("Select a port");
		String[] PortsFound = readPortNames();
		portList.removeAllItems();
		for(String string: PortsFound) {
			portList.addItem(string);
		}
		
		JButton btnRefresh = new JButton("");
		btnRefresh.setBounds(10, 31, 20, 20);
		iconimg = new ImageIcon(this.getClass().getResource("/refreshicon.png")).getImage().getScaledInstance((int)(btnRefresh.getWidth()*0.75),(int)(btnRefresh.getHeight()*0.75), java.awt.Image.SCALE_SMOOTH);
		btnRefresh.setIcon(new ImageIcon(iconimg));
		ConnPane.add(btnRefresh);
		
		JLayeredPane logPane = new JLayeredPane();
		logPane.setBounds(10, 125, 230, 154);
		logPane.setBorder(new TitledBorder(null, "Log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		frmOpticalHyperthermiaGui.getContentPane().add(logPane);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 21, 210, 86);
		logPane.add(scrollPane);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(txtLog);
		txtLog.setEditable(false);
		
		JCheckBox chckbxAutoscroll = new JCheckBox("Autoscroll");
		chckbxAutoscroll.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if(chckbxAutoscroll.isSelected()) {
					txtLog.setCaretPosition(txtLog.getDocument().getLength());
					caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
				} else {
					caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
				}
			}
		});
		chckbxAutoscroll.setSelected(true);
		chckbxAutoscroll.setBounds(10, 124, 97, 23);
		logPane.add(chckbxAutoscroll);
		
		JButton btnSaveLog = new JButton("");
		btnSaveLog.setBounds(196, 124, 24, 23);
		iconimg = new ImageIcon(this.getClass().getResource("/saveicon.png")).getImage().getScaledInstance((int)(btnSaveLog.getWidth()*0.75),(int)(btnSaveLog.getHeight()*0.75), java.awt.Image.SCALE_SMOOTH);
		btnSaveLog.setIcon(new ImageIcon(iconimg));
		logPane.add(btnSaveLog);
		btnSaveLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(Calendar.getInstance().getTime());
					FileWriter fw = new FileWriter("LOG_"+timeStamp+".txt",false);
					fw.write(txtLog.getText());
					fw.flush();
					fw.close();
					fw = null;
					txtLog.append("Log saved\r\n");
				} catch (IOException e1) {
					txtLog.append("Error writing Log\r\n");
				}
			}
		});
		
		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setBorder(new TitledBorder(null, "Experiment Variables", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		layeredPane.setBounds(250, 11, 174, 331);
		frmOpticalHyperthermiaGui.getContentPane().add(layeredPane);
		
		JLayeredPane layeredPane_1 = new JLayeredPane();
		layeredPane_1.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Time", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		layeredPane_1.setBounds(10, 23, 154, 56);
		layeredPane.add(layeredPane_1);
		
		JSpinner minutesSpinner = new JSpinner();
		minutesSpinner.setModel(new SpinnerNumberModel(10, 0, 254, 1));
		minutesSpinner.setBounds(10, 27, 50, 20);
		layeredPane_1.add(minutesSpinner);
		
		
		JSpinner secondsSpinner = new JSpinner();
		secondsSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
		secondsSpinner.setBounds(70, 27, 50, 20);
		layeredPane_1.add(secondsSpinner);
		
		JLabel lblMin = new JLabel("min");
		lblMin.setBounds(10, 11, 23, 14);
		layeredPane_1.add(lblMin);
		
		JLabel lblSec = new JLabel("sec");
		lblSec.setBounds(70, 11, 29, 14);
		layeredPane_1.add(lblSec);
		
		
		JLayeredPane layeredPane_2 = new JLayeredPane();
		layeredPane_2.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Power (100*W/cm^2)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		layeredPane_2.setBounds(10, 84, 154, 56);
		layeredPane.add(layeredPane_2);

		JSpinner powerSpinner = new JSpinner();
		powerSpinner.setModel(new SpinnerNumberModel(new Integer(40), new Integer(1), null, new Integer(1)));
		powerSpinner.setBounds(29, 25, 64, 20);
		layeredPane_2.add(powerSpinner);
		
		JCheckBox chckbxAlarm = new JCheckBox("Alarm");
		chckbxAlarm.setSelected(true);
		chckbxAlarm.setBounds(10, 227, 97, 23);
		layeredPane.add(chckbxAlarm);
		
		JLayeredPane layeredPane_3 = new JLayeredPane();
		layeredPane_3.setBorder(new TitledBorder(null, "PWM", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		layeredPane_3.setBounds(10, 151, 154, 56);
		layeredPane.add(layeredPane_3);
		
		JSpinner highSpinner = new JSpinner();
		highSpinner.setModel(new SpinnerNumberModel(1000, 0, 5000, 10));
		highSpinner.setBounds(10, 25, 63, 20);
		layeredPane_3.add(highSpinner);
		
		JSpinner lowSpinner = new JSpinner();
		lowSpinner.setModel(new SpinnerNumberModel(0, 0, 5000, 10));
		lowSpinner.setBounds(81, 25, 63, 20);
		layeredPane_3.add(lowSpinner);
		
		
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					btnStart.setBorder(BorderFactory.createLineBorder(Color.red));
					btnStop.setBorder(BorderFactory.createLineBorder(Color.green));
					btnStart.setEnabled(true);
					btnStop.setEnabled(false);
					btnMove.setEnabled(true);
					btnZero.setEnabled(true);
					experimentTimer.cancel();
					experimentTimer.purge();
					experimentTimer = null;
					char[] STOP = {0x61, 0x63, 0xFF};
					String strSTOP = new String(STOP);
					connectedPort.writeString(strSTOP);
					txtLog.append("Experiment Manual Stop\r\n");
				} catch(Exception e) {
					txtLog.append("Stop Experiment Error\r\n");
				}
			}
		});
		
		btnStop.setEnabled(false);
		btnStop.setBorder(BorderFactory.createLineBorder(Color.green));
		btnStop.setBounds(41, 291, 89, 23);
		layeredPane.add(btnStop);
		
		
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// TODO: send parameters and start experiment
				//char foo=1, bar=1;
				int minutes = (int) minutesSpinner.getValue();
				timeLeftMinutes = minutes;
				int seconds = (int) secondsSpinner.getValue();
				timeLeftSeconds = seconds;
				int power = (int) powerSpinner.getValue();
				int hightime = (int) highSpinner.getValue();
				char high2 = (char) (0x00FF&hightime);
				char high1 = (char)((0xFF00&hightime)>>8);
				int lowtime = (int) lowSpinner.getValue();
				char low2 = (char) (0x00FF&lowtime);
				char low1 = (char) ((0xFF00&lowtime)>>8);
				char alarm;
				if(chckbxAlarm.isSelected()) {
					alarm = 0x01;
				} else {
					alarm = 0x00;
				}
				char[] EXPTIM = {0x61,0x6D,(char)minutes,(char)seconds,0xFF};
				char[] EXPPOW = {0x61,0x6C,(char)power,0xFF};
				char[] EXPPWM = {0x61,0x74,high1,high2,low1,low2,0xFF};
				char[] EXPALM = {0x61,0x62,alarm,0xFF};
				char[] EXPSTR = {0x61,0x61,0xFF};
				String strEXPTIM = new String(EXPTIM);
				String strEXPPOW = new String(EXPPOW);
				String strEXPPWM = new String(EXPPWM);
				String strEXPALM = new String(EXPALM);
				String strEXPSTR = new String(EXPSTR);
				try {
					connectedPort.writeString(strEXPTIM);
					TimeUnit.MILLISECONDS.sleep(10);
					connectedPort.writeString(strEXPPOW);
					TimeUnit.MILLISECONDS.sleep(10);
					connectedPort.writeString(strEXPPWM);
					TimeUnit.MILLISECONDS.sleep(10);
					connectedPort.writeString(strEXPALM);
					TimeUnit.MILLISECONDS.sleep(10);
					connectedPort.writeString(strEXPSTR);
					TimeUnit.MILLISECONDS.sleep(10);
					btnStop.setEnabled(true);
					btnStart.setEnabled(false);
					btnStart.setBorder(BorderFactory.createLineBorder(Color.green));
					btnStop.setBorder(BorderFactory.createLineBorder(Color.yellow));
					btnMove.setEnabled(false);
					btnZero.setEnabled(false);
					experimentTimer = new Timer();
					experimentTimer.schedule(new timerTask(), 0,1000);
					txtLog.append("Experiment Start\r\n");
				} catch(SerialPortException E) {
					txtLog.append("Error writing\r\n");
				} catch(Exception e) {
					txtLog.append("Experiment Start Error");
				}
			}
		});
		btnStart.setEnabled(false);
		btnStart.setBorder(BorderFactory.createLineBorder(Color.red));
		btnStart.setBounds(41, 257, 89, 23);
		layeredPane.add(btnStart);
		
		JLabel lblHigh = new JLabel("Cycle");
		lblHigh.setBounds(10, 11, 46, 14);
		layeredPane_3.add(lblHigh);
		
		JLabel lblLow = new JLabel("Low");
		lblLow.setBounds(81, 11, 46, 14);
		layeredPane_3.add(lblLow);
		
		JLayeredPane layeredPane_4 = new JLayeredPane();
		layeredPane_4.setBorder(new TitledBorder(null, "Camera", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		layeredPane_4.setBounds(434, 11, 210, 291);
		frmOpticalHyperthermiaGui.getContentPane().add(layeredPane_4);
		
		
		camerapanel.setBounds(10, 22, 190, 190);
		layeredPane_4.add(camerapanel);
		
		JButton cameraStart = new JButton("Start");
		cameraStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(!cameraIsStarted) {
					try {
						webSource = new VideoCapture((Integer) camNumber.getValue());
						if (webSource.isOpened()) {
							cameraIsStarted = true;
							cameraTimer = new Timer();
							cameraTimer.schedule(new CameraTask(), 0, 33);
						}
					} catch (Exception e) {
						txtLog.append("Camera not available\r\n");
					}
				} else txtLog.append("Camera still ON!\r\n");
			}
		});
		cameraStart.setBounds(10, 223, 89, 23);
		layeredPane_4.add(cameraStart);
		
		JButton cameraStop = new JButton("Stop");
		cameraStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(cameraIsStarted) {
					try {
						cameraTimer.cancel();
						cameraTimer.purge();
						cameraTimer = null;
						webSource.release();
						webSource = null;
						cameraIsStarted = false;
					} catch (Exception e) {
						txtLog.append("Something went wrong\r\n");
					}
				} else txtLog.append("Camera already OFF!\r\n");
			}
		});
		cameraStop.setBounds(10, 257, 89, 23);
		layeredPane_4.add(cameraStop);
		
		camNumber.setModel(new SpinnerNumberModel(0, 0, 5, 1));
		camNumber.setBounds(119, 258, 68, 20);
		layeredPane_4.add(camNumber);
		
		JLabel lblCameraNumber = new JLabel("Number:");
		lblCameraNumber.setBounds(119, 232, 68, 14);
		layeredPane_4.add(lblCameraNumber);
		
		JLayeredPane layeredPane_5 = new JLayeredPane();
		layeredPane_5.setBorder(new TitledBorder(null, "Motors", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		layeredPane_5.setBounds(10, 290, 230, 151);
		frmOpticalHyperthermiaGui.getContentPane().add(layeredPane_5);
		
		JSpinner spinnerXPos = new JSpinner();
		spinnerXPos.setModel(new SpinnerNumberModel(1, 1, 12, 1));
		spinnerXPos.setBounds(23, 38, 46, 20);
		layeredPane_5.add(spinnerXPos);
		
		JSpinner spinnerYPos = new JSpinner();
		spinnerYPos.setModel(new SpinnerListModel(new String[] {"A", "B", "C", "D", "E", "F", "G", "H"}));
		spinnerYPos.setBounds(79, 38, 46, 20);
		layeredPane_5.add(spinnerYPos);
		
		JLabel lblXpos = new JLabel("XPos");
		lblXpos.setBounds(23, 21, 46, 14);
		layeredPane_5.add(lblXpos);
		
		JLabel lblYpos = new JLabel("YPos");
		lblYpos.setBounds(79, 21, 46, 14);
		layeredPane_5.add(lblYpos);
		
		
		btnMove.setEnabled(false);
		btnMove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					int XPos = (int) spinnerXPos.getValue();
					int YPos = (int)((spinnerYPos.getValue().toString()).charAt(0))-64;
					char[] MOTORPOS = {0x61,0x4D,(char)XPos,(char)YPos,0xFF};
					String str = new String(MOTORPOS);
					connectedPort.writeString(str);
					txtLog.append("Moving table\r\n");
				}catch (Exception e){
					txtLog.append("Communication error\r\n");
				}
			}
		});
		btnMove.setBounds(23, 78, 89, 23);
		layeredPane_5.add(btnMove);
		
		JButton btnStopM = new JButton("STOP");
		btnStopM.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		btnStopM.setEnabled(false);
		btnStopM.setBounds(23, 112, 89, 23);
		layeredPane_5.add(btnStopM);
		
		
		btnZero.setEnabled(false);
		btnZero.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					char[] MOTORPOS = {0x61,0x4D,(char)0,(char)0,0xFF};
					String str = new String(MOTORPOS);
					connectedPort.writeString(str);
					txtLog.append("Moving table\r\n");
				}catch (Exception e){
					txtLog.append("Communication error\r\n");
				}
			}
		});
		btnZero.setBounds(159, 78, 23, 23);
		iconimg = new ImageIcon(this.getClass().getResource("/homeicon.png")).getImage().getScaledInstance(btnZero.getWidth(),btnZero.getHeight(), java.awt.Image.SCALE_SMOOTH);
		btnZero.setIcon(new ImageIcon(iconimg));
		layeredPane_5.add(btnZero);
		
		JButton btnMtrUp = new JButton("");
		btnMtrUp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					char[] MOTORMAN = {0x61,0x4E,(char)1,(char)0,(char)0,(char)0,0xFF};
					String str = new String(MOTORMAN);
					connectedPort.writeString(str);
					txtLog.append("Moving up\r\n");
				} catch(Exception e) {
					txtLog.append("Communication error\r\n");
				}
				
			}
		});
		btnMtrUp.setEnabled(false);
		btnMtrUp.setBounds(159, 44, 23, 23);
		layeredPane_5.add(btnMtrUp);
		
		JButton btnMtrRt = new JButton("");
		btnMtrRt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					char[] MOTORMAN = {0x61,0x4E,(char)0,(char)0,(char)0,(char)1,0xFF};
					String str = new String(MOTORMAN);
					connectedPort.writeString(str);
					txtLog.append("Moving right\r\n");
				} catch(Exception arg0) {
					txtLog.append("Communication error\r\n");
				}
			}
		});
		btnMtrRt.setEnabled(false);
		btnMtrRt.setBounds(192, 78, 23, 23);
		layeredPane_5.add(btnMtrRt);
		
		JButton btnMtrDn = new JButton("");
		btnMtrDn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					char[] MOTORMAN = {0x61,0x4E,(char)0,(char)1,(char)0,(char)0,0xFF};
					String str = new String(MOTORMAN);
					connectedPort.writeString(str);
					txtLog.append("Moving down\r\n");
				} catch(Exception arg0) {
					txtLog.append("Communication error\r\n");
				}
			}
		});
		btnMtrDn.setEnabled(false);
		btnMtrDn.setBounds(159, 112, 23, 23);
		layeredPane_5.add(btnMtrDn);
		
		JButton btnMtrLft = new JButton("");
		btnMtrLft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					char[] MOTORMAN = {0x61,0x4E,(char)0,(char)0,(char)1,(char)0,0xFF};
					String str = new String(MOTORMAN);
					connectedPort.writeString(str);
					txtLog.append("Moving left\r\n");
				} catch(Exception arg0) {
					txtLog.append("Communication error\r\n");
				}
			}
		});
		btnMtrLft.setEnabled(false);
		btnMtrLft.setBounds(126, 78, 23, 23);
		layeredPane_5.add(btnMtrLft);
		
		
		timeLeftLbl.setFont(new Font("Tahoma", Font.PLAIN, 27));
		timeLeftLbl.setBounds(162, 46, 78, 40);
		frmOpticalHyperthermiaGui.getContentPane().add(timeLeftLbl);
		
		JLayeredPane layeredPane_6 = new JLayeredPane();
		layeredPane_6.setBorder(new TitledBorder(null, "Variables Reading", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		layeredPane_6.setBounds(434, 313, 210, 128);
		frmOpticalHyperthermiaGui.getContentPane().add(layeredPane_6);
		
		JLabel lblAdc = new JLabel("VPD");
		lblAdc.setBounds(10, 21, 35, 14);
		layeredPane_6.add(lblAdc);
		
		ADC0Value = new JTextField();
		ADC0Value.setEditable(false);
		ADC0Value.setBounds(44, 18, 43, 20);
		layeredPane_6.add(ADC0Value);
		ADC0Value.setColumns(10);
		
		ADC1Value = new JTextField();
		ADC1Value.setEditable(false);
		ADC1Value.setColumns(10);
		ADC1Value.setBounds(44, 44, 43, 20);
		layeredPane_6.add(ADC1Value);
		
		JLabel lblAdc_1 = new JLabel("PLD");
		lblAdc_1.setBounds(10, 47, 35, 14);
		layeredPane_6.add(lblAdc_1);
		
		JLabel lblAdc_2 = new JLabel("ID");
		lblAdc_2.setBounds(10, 72, 35, 14);
		layeredPane_6.add(lblAdc_2);
		
		ADC2Value = new JTextField();
		ADC2Value.setEditable(false);
		ADC2Value.setColumns(10);
		ADC2Value.setBounds(44, 69, 43, 20);
		layeredPane_6.add(ADC2Value);
		
		T1value = new JTextField();
		T1value.setEditable(false);
		T1value.setColumns(10);
		T1value.setBounds(147, 18, 43, 20);
		layeredPane_6.add(T1value);
		
		JLabel lblT = new JLabel("T\u00AA amb");
		lblT.setBounds(108, 21, 35, 14);
		layeredPane_6.add(lblT);
		
		T2value = new JTextField();
		T2value.setEditable(false);
		T2value.setColumns(10);
		T2value.setBounds(147, 44, 43, 20);
		layeredPane_6.add(T2value);
		
		JLabel lblT_1 = new JLabel("T\u00AA LD");
		lblT_1.setBounds(108, 47, 29, 14);
		layeredPane_6.add(lblT_1);
		
		textField = new JTextField();
		textField.setText("1064");
		textField.setEditable(false);
		textField.setColumns(10);
		textField.setBounds(44, 97, 43, 20);
		layeredPane_6.add(textField);
		
		JLayeredPane layeredPane_7 = new JLayeredPane();
		layeredPane_7.setBorder(new TitledBorder(null, "Temperatures", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		layeredPane_7.setBounds(250, 353, 174, 88);
		frmOpticalHyperthermiaGui.getContentPane().add(layeredPane_7);
		
		JSpinner spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(37, 25, 45, 1));
		spinner.setBounds(10, 27, 51, 20);
		layeredPane_7.add(spinner);
		
		JSpinner spinner_1 = new JSpinner();
		spinner_1.setModel(new SpinnerNumberModel(15, -10, 30, 1));
		spinner_1.setBounds(81, 27, 51, 20);
		layeredPane_7.add(spinner_1);
		
		JLabel lblAmbient = new JLabel("Ambient");
		lblAmbient.setBounds(10, 13, 46, 14);
		layeredPane_7.add(lblAmbient);
		
		JLabel lblDiode = new JLabel("Diode");
		lblDiode.setBounds(81, 13, 46, 14);
		layeredPane_7.add(lblDiode);
		
		JButton btnSet = new JButton("Set");
		btnSet.setEnabled(false);
		btnSet.setBounds(20, 58, 89, 23);
		layeredPane_7.add(btnSet);
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				portList.removeAllItems();
				String[] PortsFound = readPortNames();
				for(String string: PortsFound) {
					portList.addItem(string);
				}
			}	
		});
		
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!connStatus) {
					connStatus = connectToPort(portList.getSelectedItem().toString());
					if(connStatus) {
						btnConnect.setText("Disconnect");
						btnConnect.setBorder(BorderFactory.createLineBorder(Color.green));
						txtLog.append("Connected\r\n");
						btnStart.setEnabled(true);
						btnMove.setEnabled(true);
						btnStopM.setEnabled(true);
						btnZero.setEnabled(true);
						btnMtrUp.setEnabled(true);
						btnMtrDn.setEnabled(true);
						btnMtrLft.setEnabled(true);
						btnMtrRt.setEnabled(true);
					}
				} else {
					connStatus = disconnectFromPort();
					if(!connStatus) {
						btnConnect.setText("Connect");
						btnConnect.setBorder(BorderFactory.createLineBorder(Color.red));
						txtLog.append("Disconnected\r\n");
						btnStart.setEnabled(false);
						btnStop.setEnabled(false);
						btnMove.setEnabled(false);
						btnZero.setEnabled(false);
						btnStopM.setEnabled(false);
						btnMtrUp.setEnabled(false);
						btnMtrDn.setEnabled(false);
						btnMtrLft.setEnabled(false);
						btnMtrRt.setEnabled(false);
					}
				}
			}
		});
		String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
		txtLog.append("Program started: "+ timeStamp+"\r\n");
	}
	
	String str="";
	private JTextField ADC0Value;
	private JTextField ADC1Value;
	private JTextField ADC2Value;
	byte[] rcvSerial;
	int serialCount = 0;
	private JTextField T1value;
	private JTextField T2value;
	private JTextField textField;
	public void serialEvent(SerialPortEvent event) {
		if(event.isRXCHAR() && event.getEventValue() > 0) {
			try {
				str += connectedPort.readString();
				if(str.indexOf("\n")>-1){
					//System.out.println("Received: "+str);
					//txtLog.append(str+"");
					parseCommands(str);
					str = "";
				}

			}
			catch(Exception e) {
				txtLog.append("Error in reception\r\n");
			}
		}
	}
	
	private void parseCommands(String str) {
		String[] parts = str.split(",");
		if(parts[0].contains("2")) {
			ADC0Value.setText(((3.3/4096)*Float.parseFloat(parts[1]))+"");
			ADC1Value.setText(Math.round(((3.3/(4096*2.7))*Float.parseFloat(parts[2]))*10000d)/10000d+"");
			ADC2Value.setText(Integer.parseInt(parts[3])+"");
			T1value.setText(Float.parseFloat(parts[4])/100.0+"");
			T2value.setText(Float.parseFloat(parts[5])/100.0+"");
			
		}
	}
	
	private class CameraTask extends TimerTask{
		@Override
		public void run() {
			try {
				Mat videoframe = new Mat();
				webSource.retrieve(videoframe);
				int type = 0;
				if(videoframe.channels()==1) {
					type = BufferedImage.TYPE_BYTE_GRAY;
				} else if (videoframe.channels()==3) {
					type = BufferedImage.TYPE_3BYTE_BGR;
				}
				BufferedImage image = new BufferedImage(videoframe.width(),videoframe.height(),type);
				WritableRaster raster = image.getRaster();
				DataBufferByte dataBuffer = (DataBufferByte)raster.getDataBuffer();
				byte[] data = dataBuffer.getData();
				videoframe.get(0, 0, data);
				Graphics g = camerapanel.getGraphics();
				g.drawImage(image, 0, 0, camerapanel.getWidth(), camerapanel.getHeight(), null);
			} catch(Exception e) {
				txtLog.append("Camera not available\r\n");
				cameraTimer.cancel();
				cameraTimer.purge();
				cameraTimer = null;
				webSource.release();
				webSource = null;
				cameraIsStarted = false;
			}
		}
	}
	
	private class timerTask extends TimerTask{
		@Override
		public void run() {
			timeLeftSeconds--;
			if(timeLeftSeconds<0) {
				timeLeftSeconds = 59;
				timeLeftMinutes--;
				if(timeLeftMinutes<0) {
					txtLog.append("Time is over\r\n");
					experimentTimer.cancel();
					experimentTimer.purge();
					experimentTimer = null;
					timeLeftSeconds=0;
					timeLeftMinutes=0;
					btnMove.setEnabled(true);
					btnStart.setEnabled(true);
					btnStop.setEnabled(false);
					btnZero.setEnabled(true);
					btnStop.setBorder(BorderFactory.createLineBorder(Color.green));
					btnStart.setBorder(BorderFactory.createLineBorder(Color.red));
				}
			}
			// Implement here the counter display
			String minString = String.format("%02d",timeLeftMinutes);
			String secString = String.format("%02d",timeLeftSeconds);
			timeLeftLbl.setText(minString+":"+secString);
		}
	}
}