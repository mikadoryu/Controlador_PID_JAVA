package controller;

public class PIDController {
    private double kp, ki, kd;
    private double integral = 0;
    private double prevError = 0;

    public PIDController(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    public double compute(double setpoint, double actual, double dt) {
        double error = setpoint - actual;
        integral += error * dt;
        double derivative = (error - prevError) / dt;
        prevError = error;
        return kp * error + ki * integral + kd * derivative;
    }
}
