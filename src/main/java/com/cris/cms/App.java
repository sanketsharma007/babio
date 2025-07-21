package com.cris.cms;

import java.util.concurrent.atomic.AtomicBoolean;

public class App {
    private static final int DEFAULT_TIMEOUT = 10;
    private static AtomicBoolean baDone = new AtomicBoolean(false);
    private static AtomicBoolean waiting = new AtomicBoolean(false);
    private static int waitTime = 0;
    private static int readyWaitTime = 0;
    private static int timeout = DEFAULT_TIMEOUT;
    private static int flage = 0;
    private static Connect bt = new Connect();

    public static void main(String[] args) {
        if (args.length > 2) {
            try {
                timeout = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                timeout = DEFAULT_TIMEOUT;
            }
        }

        Thread baThread = new Thread(() -> tayalBreathThread(args));
        Thread timerThread = new Thread(() -> timer());

        baThread.start();
        timerThread.start();

        try {
            baThread.join();
            timerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bt.closePort();
    }

    // Timer thread: aborts if user does not start blowing in specified timeout seconds
    private static void timer() {
        while (!baDone.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            readyWaitTime++;
            if (waiting.get()) {
                waitTime++;
            } else {
                waitTime = 0;
            }
            if (waitTime > timeout) {
                bt.turnOff();
                bt.enable();
                waiting.set(false);
                baDone.set(true);
                System.out.println("TimeOut");
            }
        }
    }

    // Finds the port to which the device is attached and opens it
    private static int getPort() {
        for (int i = 0; i <= 30; i++) {
            String portName = "/dev/ttyUSB" + i; // Adjust for Windows, e.g., COM1, COM2, ...
            if (bt.openPort(portName)) {
                return i;
            }
        }
        return -1;
    }

    // Main BA thread logic
    private static void tayalBreathThread(String[] args) {
        int port = getPort();
        if (port == -1) {
            System.out.println("NOOPEN");
            waiting.set(false);
            baDone.set(true);
            bt.turnOff();
            return;
        }

        if (!bt.isOn()) {
            bt.turnOn();
            try { Thread.sleep(7000); } catch (InterruptedException e) {}
        }

        bt.connectpc();
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        if (!bt.checkReady()) {
            System.out.println("ERROR");
            waiting.set(false);
            baDone.set(true);
            bt.turnOff();
            return;
        }

        waiting.set(true);
        int value = bt.readBreath();
        if (value == -1) {
            System.out.println("Problem reading BA device !");
        } else {
            if (value > 0) {
                flage = 5;
            } else {
                flage = 0;
            }
            bt.enable();
        }
        baDone.set(true);
    }
}