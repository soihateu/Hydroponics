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
int heatRelay = 7;
int lightRelay = 8;
int pumpRelay = 9;
int fanRelay = 10;

// Setup for DHT sensor
#define DHTPIN 6
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

int setTemp = 25;
int setHumid = 60;
int setLight = 1000;

void setup() {
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
  while (!SI1145.Begin()) {
    Serial.println("Si1145 is not ready!");
    delay(1000);
  }
  Serial.println("Si1145 is ready!");
}

void loop()
{
  // Initialize floats for all sensor reading values plus set delay timer
  int tempReading, humidReading, visReading;
  int delayTime = 2000;

  // Read temperature
  tempReading = readTemp();
  delay(delayTime);

  // Read humidity
  humidReading = readHumid();
  delay(delayTime);

  // Read light
  visReading = readVis();

  String input = String(tempReading) + "," + String(humidReading) + "," + String(visReading) + "," + String(setTemp) + "," + String(setHumid) + "," + String(setLight);
  Serial.println("Sending string: " + input);

  for (int i = 0; i < input.length(); i++)
  {
    btSerial.write(input.charAt(i));
  }

  String output = "";

  while (btSerial.available())
  {
    char c = btSerial.read();
    output = output + c;
  }

  if (output.length() > 0)
  {
    // Get threshold values
    int numberOfSkippedValues = 3;
    bool bSetTemp = false;
    bool bSetHumid = false;
    bool bSetLight = false;
    char* pch  = strtok(&output[0], ",");

    while (pch != NULL)
    {
      // Skip first few values, as they are sensor values
      if (numberOfSkippedValues > 0)
      {
        numberOfSkippedValues--;
      }
      else
      {
        if (!bSetTemp)
        {
          bSetTemp = true;
          setTemp = atoi(pch);
        }
        else if (!bSetHumid)
        {
          bSetHumid = true;
          setHumid = atoi(pch);
        }
        else if (!bSetLight)
        {
          bSetLight = true;
          setLight = atoi(pch);
        }
        else
        {
          Serial.println("Read unknown set value!");
        }
      }

      pch = strtok(NULL, ",");
    }
  }

  if (tempReading < setTemp)
  {
    Serial.println("Temperature is low, turning off fans and turning on heat...");
    digitalWrite(fanRelay, HIGH);
    digitalWrite(heatRelay, LOW);
  }
  else if (tempReading > setTemp)
  {
    Serial.println("Temperature is high, turning on fans and turning off heat...");
    digitalWrite(fanRelay, LOW);
    digitalWrite(heatRelay, HIGH);
  }
  else if (tempReading == setTemp)
  {
    Serial.println("Temperature is perfect, turning off fans and turning off heat...");
    digitalWrite(fanRelay, HIGH);
    digitalWrite(heatRelay, HIGH);
  }

  if (visReading > setLight)
  {
    Serial.println("Light is sufficient, turning off LEDs...");
    digitalWrite(lightRelay, HIGH);
  }
  else if (visReading <= setLight)
  {
    Serial.println("Light is insufficient, turning on LEDs...");
    digitalWrite(lightRelay, LOW);
  }

  if (humidReading < setHumid)
  {
    Serial.println("Humidity is low, turning off fans and turning on pump for a bit...");
    digitalWrite(fanRelay, HIGH);
    digitalWrite(pumpRelay, LOW);
    delay(10000);
    digitalWrite(pumpRelay, HIGH);
  }
  else if (humidReading > setHumid)
  {
    Serial.println("Humidity is high, turning on fans...");
    digitalWrite(fanRelay, LOW);
  }
  else if (humidReading == setHumid)
  {
    Serial.println("Humidity is perfect, turning off fans...");
    digitalWrite(fanRelay, HIGH);
  }
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
  Serial.println("Visible Light: " + String(visibleLight));
  
  return (int)visibleLight;
}
