package com.cris.cms;

import purejavacomm.*;
import java.io.*;
import java.util.Arrays;

public class Connect {
    private SerialPort serialPort;
    private InputStream in;
    private OutputStream out;
    private byte[] sbuf = new byte[20];
    private byte[] sbuf1 = new byte[255];
    private int byt;
    private String lastStatus = "";
    private StringBuilder deviceOutput = new StringBuilder();

    public Connect() {
    }

    public boolean openPort(String portName) {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
            CommPort commPort = portIdentifier.open("BADevice", 2000);
            if (commPort instanceof SerialPort) {
                serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error opening port: " + e.getMessage());
        }
        return false;
    }

    public void closePort() {
        try {
            if (serialPort != null)
                serialPort.close();
        } catch (Exception e) {
            System.err.println("Error closing port: " + e.getMessage());
        }
    }

    public int readData(int nbytes) {
        try {
            int bytesRead = in.read(sbuf1, 0, nbytes);
            return bytesRead;
        } catch (IOException e) {
            System.err.println("Read error: " + e.getMessage());
            return -1;
        }
    }

    public void writeData(byte[] data, int nbytes) {
        try {
            out.write(data, 0, nbytes);
            out.flush();
        } catch (IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }
    }

    public int readBreath() {
        boolean waiting = true;
        int waitTime = 0;
        while (waiting) {
            Arrays.fill(sbuf1, (byte) 0);
            int bytes = readData(255);
            if (bytes > 0) {
                if (sbuf1[3] == (byte) 0x96) {
                    lastStatus = "Blowing";
                    System.out.println("Blowing");
                    return 1;

                } else if (sbuf1[3] == (byte) 0x91) {
                    lastStatus = "Blow Failure";
                    System.out.println("Blow Failure. Please blow continuously for 3 seconds into the BA device");
                    waitTime = 0;
                    return 2;
                } else if (sbuf1[3] == (byte) 0x92) {
                    lastStatus = "Analyzing";
                    System.out.println("Analyzing ... ");
                    waitTime = -30;
                    return 3;
                } else if (sbuf1[0] == (byte) 0xA4 && sbuf1[1] == (byte) 0x43) {
                    lastStatus = "Complete";
                    return 0;
                } else {
                    for (int i = 0; i < bytes; i++) {
                        byte in1 = sbuf1[i];
                        if (in1 == 10 || (in1 > 31 && in1 < 125)) {
                            deviceOutput.append((char) in1);
                        }
                    }
                }
                String str = new String(sbuf1, 0, bytes);
                if (str.contains("time")) {
                    lastStatus = "Complete";
                    waiting = false;
                    return 0;
                }
            }

        }
        return 0;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public String getDeviceOutput() {
        return deviceOutput.toString();
    }

    public void clearDeviceOutput() {
        deviceOutput.setLength(0);
    }

    public void readReady() {
        while (true) {
            String dd = read1();
            if (dd.contains("132 1")) {
                System.out.println("Please blow into the BA device");
                return;
            }
        }
    }

    public boolean checkReady() {
        Arrays.fill(sbuf, (byte) 0);
        sbuf[0] = 0x10;
        sbuf[1] = 0x04;
        sbuf[2] = (byte) 0xA0;
        sbuf[3] = 0x18;
        sbuf[4] = 0x00;
        sbuf[5] = 0x00;
        sbuf[6] = 0x00;
        sbuf[7] = (byte) 0x8D;
        sbuf[8] = (byte) 0xFC;
        sbuf[9] = 0x10;
        sbuf[10] = 0x05;
        writeData(sbuf, 11);

        int readyWaitTime = 0;
        while (readyWaitTime < 20) {
            Arrays.fill(sbuf1, (byte) 0);
            int bytes = readData(8);
            if (sbuf1[3] == 0x18) {
                System.out.println("Ready");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                System.out.println("Please blow into the BA device");
                return true;
            }
            readyWaitTime++;
        }
        System.out.println("NoReady");
        return false;
    }

    public boolean isOn() {
        Arrays.fill(sbuf, (byte) 0);
        sbuf[0] = 0x10;
        sbuf[1] = 0x04;
        sbuf[2] = (byte) 0xA0;
        sbuf[3] = 0x22;
        sbuf[4] = 0x00;
        sbuf[5] = 0x00;
        sbuf[6] = 0x00;
        sbuf[7] = (byte) 0x81;
        sbuf[8] = 0x24;
        sbuf[9] = 0x10;
        sbuf[10] = 0x05;
        writeData(sbuf, 11);

        int readyWaitTime = 0;
        while (readyWaitTime < 10) {
            Arrays.fill(sbuf1, (byte) 0);
            int bytes = readData(255);
            if (sbuf1[8] == 0x38 && sbuf1[9] == 0x7A) {
                System.out.println("Device is Off. Now Turning On");
                return false;
            } else if (sbuf1[8] == (byte) 0xF9 && sbuf1[9] == (byte) 0xBA) {
                return true;
            }
            readyWaitTime++;
        }
        return false;
    }

    public void writeData(int nbytes) {
        writeData(sbuf, nbytes);
    }

    public String read1() {
        int bytes = readData(255);
        byt = bytes;
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < bytes; i++) {
            value.append(" ").append(Byte.toUnsignedInt(sbuf1[i]));
        }
        value.append("\n ");
        return value.toString();
    }

    public int check() {
        Arrays.fill(sbuf, (byte) 0);
        sbuf[0] = (byte) 0x80;
        sbuf[1] = (byte) 0xAA;
        writeData(sbuf, 2);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        String dd = read1();
        if (dd.contains("79 78")) {
            return 0;
        } else {
            return 1;
        }
    }

    public void enable() {
        Arrays.fill(sbuf, (byte) 0);
        sbuf[0] = (byte) 0x81;
        sbuf[1] = 0x5A;
        sbuf[2] = 0x00;
        writeData(sbuf, 3);
    }

    public void disconnect() {
        Arrays.fill(sbuf, (byte) 0);
        sbuf[0] = (byte) 0xA6;
        sbuf[1] = 0x5A;
        writeData(sbuf, 2);
    }

    public void connectpc() {
        Arrays.fill(sbuf, (byte) 0);
        sbuf[0] = 0x10;
        sbuf[1] = 0x04;
        sbuf[2] = (byte) 0xA0;
        sbuf[3] = 0x17;
        sbuf[4] = 0x00;
        sbuf[5] = 0x00;
        sbuf[6] = 0x00;
        sbuf[7] = (byte) 0x8E;
        sbuf[8] = (byte) 0xE8;
        sbuf[9] = 0x10;
        sbuf[10] = 0x05;
        writeData(sbuf, 11);
    }

    public void turnOn() {
        Arrays.fill(sbuf, (byte) 0);
        System.out.println("Turning On. Please Wait...");
        sbuf[0] = 0x10;
        sbuf[1] = 0x04;
        sbuf[2] = (byte) 0xA0;
        sbuf[3] = 0x11;
        sbuf[4] = 0x00;
        sbuf[5] = 0x00;
        sbuf[6] = 0x00;
        sbuf[7] = (byte) 0x8E;
        sbuf[8] = 0x60;
        sbuf[9] = 0x10;
        sbuf[10] = 0x05;
        writeData(sbuf, 11);
    }

    public void turnOff() {
        Arrays.fill(sbuf, (byte) 0);
        sbuf[0] = 0x10;
        sbuf[1] = 0x04;
        sbuf[2] = (byte) 0xA0;
        sbuf[3] = 0x12;
        sbuf[4] = 0x00;
        sbuf[5] = 0x00;
        sbuf[6] = 0x00;
        sbuf[7] = (byte) 0x8E;
        sbuf[8] = 0x24;
        sbuf[9] = 0x10;
        sbuf[10] = 0x05;
        writeData(sbuf, 11);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }
}