// Bluetooth HM10 RX -> TX (pin 46 on Mega2560)
// Bluetooth HM10 TX -> RX (pin 48 on Mega2560)
// Remember to use pins specifically meant for AltSoftSerial
// Format of characteristic: ("temperature, humidity, lighting (visible light), IR, UV, tempThreshold, HumidThreshold, LightThreshold")
// REMEMBER TO USE NO LINE ENDING ****
// Characteristic cannot hold more than 64 characters, will overflow and output incorrect values. (IE. 20,100,100,30,100,100,100)

// All includes for sensors
#include <DHT.h>
#include <Wire.h>
#include "Arduino.h"
#include "SI114X.h"
#include <AltSoftSerial.h>

// Setup for light sensor
SI114X SI1145 = SI114X();

// Bluetooth HM10 RX -> TX (pin 46 on Mega2560)
// Bluetooth HM10 TX -> RX (pin 48 on Mega2560)
// Remember to use pins meant for AltSoftSerial
AltSoftSerial btSerial;

// Digital ports for all relays
int heatRelay = 4;
int lightRelay = 5;
int pumpRelay = 6;
int fanRelay = 7;

// Setup for DHT sensor
#define DHTPIN 3
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

int setTemp = 25;
int setHumid = 60;
int setLight = 40;

String btSerialBuffer = "";

void setup()
{
  // Initialize DHT sensor, start the Serial
  dht.begin();
  Serial.begin(9600);

  // Starting Bluetooth serial
  btSerial.begin(9600);
  Serial.println("btSerial started at 9600");

  // Setting up relays using pinMode
  pinMode(heatRelay, OUTPUT);
  pinMode(lightRelay, OUTPUT);
  pinMode(pumpRelay, OUTPUT);
  pinMode(fanRelay, OUTPUT);

  // Start up light sensor, if not ready then keep waiting till ready
  while (!SI1145.Begin())
  {
    Serial.println("Si1145 is not ready!");
    delay(1000);
  }

  Serial.println("Si1145 is ready!");
}

void loop()
{
  // Initialize floats for all sensor reading values plus set delay timer
  int tempReading, humidReading, visReading;
  int delayTime = 1000;

  bool updateValues = false;

  // Read set value operations from bluetooth module
  while (btSerial.available() > 0)
  {
    char c = btSerial.read();

    if (c != '\n')
    {
      btSerialBuffer += c;
    }
    else
    {
      updateValues = true;
      break;
    }
  }

  if (updateValues)
  {
    Serial.println("btSerialBuffer is: " + btSerialBuffer);

    // Get threshold values
    bool bSetTemp = false;
    bool bSetHumid = false;
    bool bSetLight = false;
    char* pch = strtok(&btSerialBuffer[0], ",");

    // Update set values from Bluetooth
    // Only updates set values if the buffer has 6 values (from the app)
    while (pch != NULL)
    {
      int operation = atoi(pch);

      if (!bSetTemp)
      {
        bSetTemp = true;

        if (operation == -1)
        {
          setTemp--;
        }
        else if (operation == 1)
        {
          setTemp++;
        }
      }
      else if (!bSetHumid)
      {
        bSetHumid = true;

        if (operation == -1)
        {
          setHumid -= 10;
        }
        else if (operation == 1)
        {
          setHumid += 10;
        }
      }
      else if (!bSetLight)
      {
        bSetLight = true;

        if (operation == -1)
        {
          setLight -= 10;
        }
        else if (operation == 1)
        {
          setLight += 10;
        }
      }
      else
      {
        Serial.println("Read unknown set value!");
      }

      pch = strtok(NULL, ",");
    }

    btSerialBuffer = "";
  }

  tempReading = readTemp();
  humidReading = readHumid();
  visReading = readVis();

  String input = String(tempReading) + ',' + String(humidReading) + ',' + String(visReading) + ',' + String(setTemp) + ',' + String(setHumid) + ',' + String(setLight) + '\n';
  Serial.println("Sending string: " + input);

  for (int i = 0; i < input.length(); i++)
  {
    btSerial.write(input.charAt(i));
  }

  if (tempReading < setTemp)
  {
    Serial.println("Temperature is low, turning off fans and turning on heat...");
    digitalWrite(fanRelay, LOW);
    digitalWrite(heatRelay, HIGH);
  }
  else if (tempReading > setTemp)
  {
    Serial.println("Temperature is high, turning on fans and turning off heat...");
    digitalWrite(fanRelay, HIGH);
    digitalWrite(heatRelay, LOW);
  }
  else if (tempReading == setTemp)
  {
    Serial.println("Temperature is perfect, turning off fans and turning off heat...");
    digitalWrite(fanRelay, LOW);
    digitalWrite(heatRelay, LOW);
  }

  if (visReading > setLight)
  {
    Serial.println("Light is sufficient, turning off LEDs...");
    digitalWrite(lightRelay, LOW);
  }
  else if (visReading <= setLight)
  {
    Serial.println("Light is insufficient, turning on LEDs...");
    digitalWrite(lightRelay, HIGH);
  }

  if (humidReading < setHumid)
  {
    Serial.println("Humidity is low, turning off fans and turning on pump for a bit...");
    digitalWrite(fanRelay, LOW);
    digitalWrite(pumpRelay, HIGH);
  }
  else if (humidReading > setHumid)
  {
    Serial.println("Humidity is high, turning on fans...");
    digitalWrite(fanRelay, HIGH);
    digitalWrite(pumpRelay, LOW);
  }
  else if (humidReading == setHumid)
  {
    Serial.println("Humidity is perfect, turning off fans...");
    digitalWrite(fanRelay, LOW);
    digitalWrite(pumpRelay, LOW);
  }

  delay(delayTime);
}

// Temperature reading function; takes value from sensor and stores it, if value is NaN then error occurred during read
int readTemp()
{
  Serial.println("Reading temperature...");

  float temp = dht.readTemperature();

  if (isnan(temp))
  {
    Serial.println("Error reading temperature!");
  }
  else
  {
    Serial.println("Temperature: " + String(temp) + "°C");
  }

  return (int)temp;
}

// Humidity reading function; takes value from sensor and stores it, if value is NaN then error occurred during read
int readHumid()
{
  Serial.println("Reading humidity...");

  float humid = dht.readHumidity();

  if (isnan(humid))
  {
    Serial.println("Error reading humidity!");
  }
  else
  {
    Serial.println("Humidity: " + String(humid) + "%");
  }

  return (int)humid;
}

// Visible light reading function; takes value from sensor and outputs it to serial
int readVis()
{
  Serial.println("Reading visible light...");

  float visibleLight = SI1145.ReadVisible();

  // Divide by 2500(Maximum light value that can be changed) to get %
  visibleLight = (visibleLight / 450) * 100;

  Serial.println("Visible Light: " + String(visibleLight));

  return (int)visibleLight;
}
