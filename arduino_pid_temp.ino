#define SERIES_RESISTOR 47000.0   // 47kΩ (ajustar según tu resistencia)
#define NOMINAL_RESISTANCE 100000.0 // 100kΩ @25°C
#define NOMINAL_TEMPERATURE 25.0   // °C
#define B_COEFFICIENT 3950.0       // Constante beta típica
#define ADC_MAX 1023.0

const int ntcPin = A7;      // Pin donde está el divisor NTC
const int pwmPin = 12;       // PWM al MOSFET (D9)
bool enviarDatos = false;
unsigned long lastSendTime = 0;
const unsigned long sendInterval = 1000; // 1 segundo entre envíos

void setup() {
  Serial.begin(9600);
  pinMode(pwmPin, OUTPUT);
}

void loop() {
  if (Serial.available() > 0) {
    byte cmd = Serial.read();

if (cmd == 0xAA) {
  Serial.write('K'); // Confirmación inicio
  delay(20);         // <-- Muy importante para evitar solapamiento en la lectura
  float temp = readTemperature();
  Serial.write((byte*)&temp, sizeof(float)); // Enviar el primer float
}
    else if (cmd == 0xAB) {
      float temp = readTemperature();
      Serial.write((byte*)&temp, sizeof(float)); // enviar 4 bytes float temperatura
    }
    else {
      // Otros comandos: por ejemplo controlar PWM
      analogWrite(pwmPin, cmd);
    }
  }
}

float readTemperature() {
  int adc = analogRead(ntcPin);
  double resistance = SERIES_RESISTOR * ((double)adc / (ADC_MAX - (double)adc));
  double steinhart;
  steinhart = resistance / NOMINAL_RESISTANCE;
  steinhart = log(steinhart);
  steinhart /= B_COEFFICIENT;
  steinhart += 1.0 / (NOMINAL_TEMPERATURE + 273.15);
  steinhart = 1.0 / steinhart;
  steinhart -= 273.15;
  return (float)steinhart;
}

void sendFloat(float f) {
  union {
    float val;
    byte bytes[4];
  } data;
  data.val = f;
  Serial.write(data.bytes, 4);
  Serial.flush();
}
