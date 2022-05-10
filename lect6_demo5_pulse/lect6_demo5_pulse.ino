#include <SPI.h>
#include <Wire.h>
#include <Adafruit_CircuitPlayground.h>

void setup() {
  //while (!Serial);
  Serial.begin(115200);
  Serial.println("Circuitplayground Pulse sensor test");
  CircuitPlayground.begin();
  CircuitPlayground.setBrightness(255);
  CircuitPlayground.setPixelColor(1, 0, 255, 0);  
}

#define numsamples 20

void loop() {
  
  uint16_t samples[numsamples];

  for (int i=0; i<numsamples; i++) {
    samples[i] = CircuitPlayground.lightSensor();

    uint16_t dc_minimum = 1024;
    for (int x=0; x<numsamples; x++) {
      dc_minimum = min(dc_minimum, samples[x]);
    }
    
    CircuitPlayground.setPixelColor(5, samples[i] - dc_minimum, 0, 0);
    Serial.println(samples[i]- dc_minimum);
    delay(10);
  }
}
