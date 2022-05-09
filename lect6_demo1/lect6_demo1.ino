

void setup() {
  // put your setup code here, to run once:
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(13, OUTPUT);
//  attachInterrupt(digitalPinToInterrupt(pin), ISR, mode); //(recommended)
}

void loop() {
  // put your main code here, to run repeatedly:
  digitalWrite(LED_BUILTIN, HIGH); //turn the LED on (HIGH is the voltage level)
  digitalWrite(13, HIGH); //turn the LED on (HIGH is the voltage level)
  delay(1000); //wait for a second
  digitalWrite(LED_BUILTIN, LOW); // turn the LED off by making the voltage LOW
//  digitalWrite(13, LOW); // turn the LED off by making the voltage LOW
  delay(1000); // wait for a second
}
