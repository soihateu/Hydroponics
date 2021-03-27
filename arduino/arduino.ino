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
	
	String str = "";
}

void loop() {
	// Initialize floats for all sensor reading values plus set delay timer
	float tempReading, humidReading, visReading;
	int delayTime = 2000;
	
	// Read temperature and store latest reading; error handling if function returns 0
	Serial.println("Reading temperature...");
	tempReading = readTemp();
	if (tempReading == 0) {
		Serial.println("Error when reading temperature");
	}
	delay(delayTime);
	str = str + tempReading + ", ";
	
	// Read humidity and store latest reading; error handling if function returns 0
	Serial.println("Reading humidity...");
	humidReading = readHumid();
	if (humidReading == 0) {
		Serial.println("Error when reading humidity");
	}
	delay(delayTime);
	str = str + humidReading + ", ";
	
	// Read different light types and store latest reading
	Serial.println("Reading Visible light...");
	visReading = readVis();
	str = str + visReading + ", ";
	
	str = str + setTemp + ", " + setHumid + ", " + setLight + ',';
	Serial.println("Sending string: " + str);
	
	int stringLength = str.length();
	for (int x = 0; x < stringLength; x++) {
		btSerial.write(str.charAt(x));
	}
	
	String strReceive = "";
	while (btSerial.available()) {
		char c = btSerial.read();
		strReceive = strReceive + c;
	}
	
	// figure out threshold values
	if ((strReceive.length()) == 0) {
		
	}
	else {
		for (int y = 0; y < (strReceive.length()); y++) {
			int commaCount = 0;
			if ((strReceive.charAt(y)) == ',') {
				commaCount++;
				if (commaCount == 3) {
					break;
				}
			}
		}
		String tempStr = strReceive.charAt(y+1) + strReceive.charAt(y+2);
		String humidStr = strReceive.charAt(y+4) + strReceive.charAt(y+5);
		int a = y + 7;
		int b = strReceive.length();
		String lightStr = "";
		for (int c = a; c < b; c++) {
			lightStr = lightStr + strReceive.charAt(c);
		}
		
		setTemp = tempStr.toInt();
		setHumid = humidStr.toInt();
		setLight = lightStr.toInt();
	}
	
	if (tempReading < setTemp) {
		Serial.println("Temperature is low, turning off fans and turning on heat...");
		digitalWrite(fanRelay, HIGH);
		digitalWrite(heatRelay, LOW);
	}
	else if (tempReading > setTemp) {
		Serial.println("Temperature is high, turning on fans and turning off heat...");
		digitalWrite(fanRelay, LOW);
		digitalWrite(heatRelay, HIGH);
	}
	else if (tempReading == setTemp) {
		Serial.println("Temperature is perfect, turning off fans and turning off heat...");
		digitalWrite(fanRelay, HIGH);
		digitalWrite(heatRelay, HIGH);
	}
	
	if (visReading > setLight) {
		Serial.println("Light is sufficient, turning off LEDs...");
		digitalWrite(lightRelay, HIGH);
	}
	else if (visReading <= setLight) {
		Serial.println("Light is insufficient, turning on LEDs...");
		digitalWrite(lightRelay, LOW);
	}
	
	if (humidReading < setHumid) {
		Serial.println("Humidity is low, turning off fans and turning on pump for a bit...");
		digitalWrite(fanRelay, HIGH);
		digitalWrite(pumpRelay, LOW);
		delay(10000);
		digitalWrite(pumpRelay, HIGH);
	}
	else if (humidReading > setHumid) {
		Serial.println("Humidity is high, turning on fans...");
		digitalWrite(fanRelay, LOW);
	}
	else if (humidReading == setHumid) {
		Serial.println("Humidity is perfect, turning off fans...");
		digitalWrite(fanRelay, HIGH);
	}
}

// Temperature reading function; takes value from sensor and stores it, if value is NaN then error occurred during read
float readTemp() {
	float temp = dht.readTemperature();
	if (isnan(temp)) {
		return 0;
	}
	else {
		Serial.println("Temperature: " + String(temp) + "°C");
		return temp;
	}
}

// Humidity reading function; takes value from sensor and stores it, if value is NaN then error occurred during read
float readHumid() {
	float humid = dht.readHumidity();
	if (isnan(humid)) {
		return 0;
	}
	else {
		Serial.println("Humidity: " + String(humid) + "%");
		return humid;
	}
}

// Visible light reading function; takes value from sensor and outputs it to serial
float readVis() {
	float visibleLight = SI1145.ReadVisible();
	Serial.println("Visible Light: " + String(visibleLight));
	return visibleLight;
}