# 🔥 Controlador PID de Temperatura

Este proyecto implementa un sistema de control de temperatura usando un algoritmo PID. Utiliza un Arduino para leer un sensor NTC (termistor), controla un elemento calefactor mediante un MOSFET y permite supervisión y ajuste en tiempo real desde una aplicación JavaFX.

---

## 📌 Objetivo

Diseñar e implementar un sistema de control automático de temperatura con un perfil configurable, utilizando comunicación serial entre un Arduino y una interfaz gráfica en JavaFX.

---

## 🛠️ Tecnologías usadas

- 🖥️ **JavaFX** — interfaz gráfica y lógica PID  
- 🔌 **jSerialComm** — comunicación UART con Arduino  
- 🔧 **Arduino UNO / Ender3 Board** — lectura NTC y control PWM  
- 📈 **Termistor NTC 100k** — sensor de temperatura  
- ⚡ **MOSFET RFP30N06LE / D522** — conmutación de potencia  
- 📁 **CSV** — para definir el perfil de temperatura  

---
## 📦 Librerías Externas

El proyecto utiliza la librería [**jSerialComm**](https://fazecast.github.io/jSerialComm/) para la comunicación UART con el Arduino.

Puedes descargarla directamente desde Maven Repository aquí:

🔗 [jSerialComm-2.11.2.jar](https://repo1.maven.org/maven2/com/fazecast/jSerialComm/2.11.2/jSerialComm-2.11.2.jar)

Para agregarla a tu proyecto manualmente:
1. Descarga el archivo JAR.
2. Agrégalo al classpath o configura tu IDE para incluirlo como librería externa.

## 📂 Estructura del proyecto

```
controlador_pid_temp/
├── src/
│   ├── controller/          # PIDController.java
│   ├── io/                  # UartManager.java
│   ├── model/               # TemperatureProfile.java
│   ├── simulator/           # HeaterSimulator.java
│   ├── view/                # MainView.java
│   └── main.java            # Clase principal (JavaFX)
├── arduino/
│   └── arduino_pid_temp.ino # Código de Arduino
├── perfil.csv               # Perfil de temperatura a seguir
├── README.md                # Este archivo
```

---

## 🚀 Instrucciones de uso

### 1. Arduino
- Conecta el termistor en un divisor resistivo (ej: 100k con 47k).
- Conecta el drenaje del MOSFET al calefactor y el source a GND.
- Sube el sketch `arduino_pid_temp.ino`.

### 2. Interfaz JavaFX
- Asegúrate de tener Java 11+ y JavaFX configurado.
### 3. Perfil CSV
- Define el setpoint en `perfil.csv` en formato:

```csv
0,25
30,60
60,200
120,180
180,25
```

