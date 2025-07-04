package simulator;

public class HeaterSimulator {
    private double temperature = 25.0;
    private final double ambient = 25.0;

    public void update(double power) {
        temperature += (power * 0.1) - (temperature - ambient) * 0.05;
    }

    public double getCurrentTemperature() {
        return temperature;
    }
}
