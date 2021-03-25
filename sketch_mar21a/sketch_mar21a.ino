#include <AltSoftSerial.h>

// Bluetooth HM10 RX -> TX (pin 46 on Mega2560)
// Bluetooth HM10 TX -> RX (pin 48 on Mega2560)
// Remember to use pins meant for AltSoftSerial
AltSoftSerial btSerial;

void setup() 
{
    Serial.begin(9600);
    btSerial.begin(9600);
    Serial.println("btSerial started at 9600");
}
 
void loop()
{
    // Read from the Bluetooth module and send to the Arduino Serial Monitor
    while (btSerial.available())
    {
        char c = btSerial.read();
        Serial.write(c);
    }

    // Read from the Serial Monitor and send to the Bluetooth module
    while (Serial.available())
    {
        char c = Serial.read();
        btSerial.write(c);
    }
}
