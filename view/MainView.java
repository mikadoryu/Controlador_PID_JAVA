package view;

import controller.PIDController;
import io.UartManager;
import javafx.application.Platform;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.TemperatureProfile;
import simulator.HeaterSimulator;
import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.concurrent.*;
// Importaciones omitidas por brevedad...

public class MainView {
    private BorderPane root;
    private LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> tempSeries;
    private XYChart.Series<Number, Number> setpointSeries;

    private ScheduledExecutorService executor;
    private int t = 0;
    private boolean loopStarted = false;

    private boolean useSimulation = true;

    private HeaterSimulator heater = new HeaterSimulator();
    private UartManager uart = new UartManager();
    private TemperatureProfile profile = new TemperatureProfile();
    private PIDController pid = new PIDController(3, 0.1, 0.2);


    private ComboBox<String> portSelector = new ComboBox<>();
    private volatile boolean loopRunning = false;

    public MainView() {
        try {
            profile.loadFromFile("perfil.csv");
        } catch (IOException e) {
            System.err.println("No se pudo cargar perfil.csv");
        }

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Temperatura en Tiempo Real");

        tempSeries = new XYChart.Series<>();
        tempSeries.setName("Temperatura Actual");

        setpointSeries = new XYChart.Series<>();
        setpointSeries.setName("Setpoint");

        chart.getData().addAll(tempSeries, setpointSeries);

        // UART port list
        for (SerialPort port : SerialPort.getCommPorts()) {
            portSelector.getItems().add(port.getSystemPortName());
        }
        portSelector.setPromptText("Seleccionar puerto");

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton simRadio = new RadioButton("Simulado");
        RadioButton uartRadio = new RadioButton("UART");
        simRadio.setToggleGroup(modeGroup);
        uartRadio.setToggleGroup(modeGroup);
        simRadio.setSelected(true);

        Button startButton = new Button("Iniciar");
        startButton.setOnAction(e -> {
            if (loopRunning) return;  // Evita reinicios simultáneos
            //loopRunning = true;

            if (uartRadio.isSelected()) {
                useSimulation = false;
                String selectedPort = portSelector.getValue();
                if (selectedPort == null || selectedPort.isEmpty()) {
                    showError("Debes seleccionar un puerto UART");
                    loopRunning = false;
                    return;
                }

                boolean ok = uart.connect(selectedPort, 9600);
                if (!ok) {
                    showError("Error abriendo el puerto.");
                    loopRunning = false;
                    return;
                }

                // Ejecuta la sincronización sin bloquear la UI
                Executors.newSingleThreadExecutor().submit(() -> {
                    try {
                        Thread.sleep(3000); // Esperar reinicio Arduino
                        uart.sendStartCommand(); // Enviar 0xAA
                        boolean confirmed = uart.waitForConfirmation(); // Esperar 'K'
                        if (!confirmed) {
                            Platform.runLater(() -> {
                                showError("No se recibió confirmación del Arduino.");
                                uart.disconnect();
                                loopRunning = false;
                            });
                            return;
                        }
                        float[] firstTemp = new float[1];
                        boolean tempOk = uart.waitForFirstTemperature(firstTemp);
                        if (!tempOk) {
                            Platform.runLater(() -> {
                                showError("No se recibió el primer dato de temperatura.");
                                uart.disconnect();
                                loopRunning = false;
                            });
                            return;
                        }
                        System.out.println("Primera temperatura recibida: " + firstTemp[0]);

                        // Iniciar ciclo si confirmado
                        Platform.runLater(() -> {
                            System.out.println("→ Iniciando loop PID desde Platform.runLater()");
                            startLoop();
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            showError("Error durante la sincronización con Arduino.");
                            uart.disconnect();
                            loopRunning = false;
                        });
                    }
                });

            } else {
                useSimulation = true;
                startLoop();
            }
        });


        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            loopRunning = false;

            t = 0;
            tempSeries.getData().clear();
            setpointSeries.getData().clear();

            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }

            if (!useSimulation) {
                uart.disconnect();
            }
        });

        Button stopButton = new Button("Detener");
        stopButton.setOnAction(e -> {
            loopRunning = false;

            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }

            if (!useSimulation) {
                try {
                    uart.sendControlSignal(0); // Apaga actuador (PWM = 0)
                    uart.disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Error al detener UART");
                }
            } else {
                heater.update(0); // Apaga simulación
            }
        });
        HBox controls = new HBox(10, startButton,stopButton, resetButton, simRadio, uartRadio, portSelector);
        root = new BorderPane(chart, null, null, controls, null);
    }

    public BorderPane getRoot() {
        return root;
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            alert.showAndWait();
        });
    }

    public void startLoop() {
        if (loopRunning) return;  // Evita múltiples ejecuciones simultáneas
        loopRunning = true;

        // Detiene cualquier ejecución previa del executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        // Cargar setpoints si aún no están graficados
        if (setpointSeries.getData().isEmpty()) {
            for (int i = 0; i <= profile.getLastTime(); i++) {
                double sp = profile.getSetpoint(i);
                setpointSeries.getData().add(new XYChart.Data<>(i, sp));
            }
        }

        try {
            executor = Executors.newSingleThreadScheduledExecutor();

            executor.scheduleAtFixedRate(() -> {
                System.out.println("→ Ciclo ejecutado en t = " + t);
                try {
                    if (!useSimulation && !uart.isConnected()) {
                        System.err.println("UART no conectado. Terminando ejecución.");
                        return;
                    }

                    double setpoint = profile.getSetpoint(t);
                    double actualTemp;

                    if (useSimulation) {
                        actualTemp = heater.getCurrentTemperature();
                    } else {
                        System.out.println("→ solicitando temperatura...");
                        uart.requestTemperature();

                        System.out.println("→ leyendo temperatura...");
                        actualTemp = uart.readTemperature();

                        System.out.println("→ temperatura leída: " + actualTemp);
                    }

                    double control = pid.compute(setpoint, actualTemp, 1.0);

                    if (useSimulation) {
                        heater.update(control);
                    } else {
                        uart.sendControlSignal(control);
                    }

                    final int time = t;
                    final double temp = actualTemp;

                    Platform.runLater(() -> {
                        tempSeries.getData().add(new XYChart.Data<>(time, temp));
                    });

                    t++;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        showError("Error en el ciclo PID:\n" + ex.getMessage());
                    });
                    loopRunning = false;
                    executor.shutdownNow(); // Detiene el ciclo si ocurre una excepción
                }
            }, 0, 1, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("ERROR al iniciar ScheduledExecutorService:");
            e.printStackTrace();
            Platform.runLater(() -> showError("No se pudo iniciar el ciclo PID:\n" + e.getMessage()));
            loopRunning = false;
        }
    }


    private void showWaitingDialog(Runnable onSuccess, Runnable onFailure) {
        Platform.runLater(() -> {
            Alert waitingAlert = new Alert(Alert.AlertType.NONE);
            waitingAlert.setTitle("Sincronizando");
            waitingAlert.setHeaderText("Esperando respuesta de Arduino...");
            waitingAlert.getDialogPane().getButtonTypes().clear(); // Quita botones

            // Mostrar diálogo modale
            waitingAlert.show();

            // Ejecutar tarea en segundo plano
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    if (uart.waitForConfirmation()) {
                        Platform.runLater(() -> {
                            waitingAlert.close();
                            onSuccess.run();
                        });
                    } else {
                        Platform.runLater(() -> {
                            waitingAlert.close();
                            onFailure.run();
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        waitingAlert.close();
                        onFailure.run();
                    });
                }
            });
        });
    }


}


