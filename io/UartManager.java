package io;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UartManager {
    private SerialPort serialPort;
    private OutputStream out;
    private InputStream in;

    public boolean connect(String portName, int baudRate) {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 2000, 0);

        if (serialPort.openPort()) {
            try {
                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                return true;
            } catch (Exception e) {
                System.err.println("Error al obtener flujos de entrada/salida: " + e.getMessage());
                return false;
            }
        } else {
            System.err.println("No se pudo abrir el puerto: " + portName);
            return false;
        }
    }

    public void sendControlSignal(double signal) throws Exception {
        if (out == null) throw new IllegalStateException("OutputStream no inicializado");
        int val = (int) Math.max(0, Math.min(255, signal));
        out.write(val);
        out.flush();
    }

    public double readTemperature() throws Exception {
        if (in == null) throw new IllegalStateException("InputStream no inicializado");

        byte[] buffer = new byte[4];
        int totalRead = 0;
        long start = System.currentTimeMillis();

        // Leer exactamente 4 bytes, con timeout de 1 s
        while (totalRead < 4) {
            if (System.currentTimeMillis() - start > 1000) {
                throw new IOException("Timeout esperando temperatura");
            }
            int r = in.read(buffer, totalRead, 4 - totalRead);
            if (r > 0) totalRead += r;
        }

        // Arduino envía el float en LITTLE ENDIAN: LSB primero, MSB al final
        int tempRaw = ((buffer[3] & 0xFF) << 24) |
                ((buffer[2] & 0xFF) << 16) |
                ((buffer[1] & 0xFF) << 8)  |
                (buffer[0] & 0xFF);

        float temperature = Float.intBitsToFloat(tempRaw);
        System.out.println("Temperatura recibida: " + temperature);
        return temperature;
    }


    public void sendStartCommand() throws Exception {
        if (out == null) {
            throw new IllegalStateException("OutputStream no inicializado");
        }
        System.out.println("Enviando comando de inicio 0xAA...");
        out.write(0xAA);
        out.flush();
        System.out.println("Comando de inicio enviado.");
    }

    public boolean waitForConfirmation() throws Exception {
        if (in == null) return false;

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 2000) {
            if (in.available() > 0) {
                int b = in.read();
                System.out.println("Recibido (confirmación): " + (char) b);
                return (b == 'K');
            }
            Thread.sleep(10);
        }
        return false;
    }

    public boolean waitForFirstTemperature(float[] firstTempOut) {
        if (in == null) return false;

        long start = System.currentTimeMillis();
        byte[] buffer = new byte[4];
        int totalRead = 0;

        try {
            while (System.currentTimeMillis() - start < 3000) {
                if (in.available() > 0) {
                    int r = in.read(buffer, totalRead, 4 - totalRead);
                    if (r > 0) {
                        totalRead += r;
                    }

                    if (totalRead == 4) {
                        // LITTLE ENDIAN
                        int tempRaw = ((buffer[3] & 0xFF) << 24) |
                                ((buffer[2] & 0xFF) << 16) |
                                ((buffer[1] & 0xFF) << 8) |
                                (buffer[0] & 0xFF);
                        float temp = Float.intBitsToFloat(tempRaw);
                        System.out.println("Primera temperatura (tras K): " + temp);
                        if (firstTempOut != null && firstTempOut.length > 0)
                            firstTempOut[0] = temp;
                        return true;
                    }
                }

                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void requestTemperature() throws Exception {
        System.out.println("→ Solicitando temperatura al Arduino (0xAB)");
        if (out == null) throw new IllegalStateException("OutputStream no inicializado");
        out.write(0xAB);
        out.flush();
    }

    public void disconnect() {
        try {
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                serialPort = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return serialPort != null && serialPort.isOpen() && in != null && out != null;
    }
}
