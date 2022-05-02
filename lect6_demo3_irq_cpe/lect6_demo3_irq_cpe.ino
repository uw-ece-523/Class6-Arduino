#include <Adafruit_CircuitPlayground.h>
volatile byte state = LOW;
const byte ledPin = 13;
const byte interruptPin = 8;

void blink() {
  state = !state;
  Serial.println("Button clicked!");
}

void setup() {
  // put your setup code here, to run once:
  CircuitPlayground.begin();
  pinMode(ledPin, OUTPUT);
  pinMode(interruptPin, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(4), blink, CHANGE); //(recommended)
  Serial.begin(9600);
  Serial.println("TAP!");
  
}

void loop() {
  // put your main code here, to run repeatedly:
  digitalWrite(ledPin, state);
}
