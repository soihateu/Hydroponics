

# Hydroponics Mobile Application

NOTE: Bluetooth BLE is not supported on android studio emulators. Enable USB debugging and run on phone.

- Characteristic cannot hold more than 64 characters, will overflow and output incorrect values. (IE. 20,100,100,30,100,100,100)

### Arduino Setup

- Uses AltSoftSerial library; make sure to use the correct ports specifically for AltSoftSerial.

- HM10 RX -> TX of the board (uses pin 46 on MEGA2560)

- HM10 TX -> RX of the board (Uses pin 48 on MEGA2560)

- Set serial monitor to "No Line Ending"

- Format of GATT characteristic: 
      ("temperature, humidity, lighting (visible light), IR, UV, tempThreshold, HumidThreshold, LightThreshold")

  ![Arduino HM-10 BLE Module Connection Circuit Diagram](https://circuitdigest.com/sites/default/files/circuitdiagram_mic/Circuit-Diagram-for-HM-10-BLE-Module-with-Arduino-to-Control-an-LED-using-Android-App_0.png)

### Application Images

##### Disconnected State

![Application Disconnected](https://github.com/soihateu/Hydroponics/blob/main/img/Hydroponic%20Disconnect.png)

##### Connected State

![Application Connected](https://github.com/soihateu/Hydroponics/blob/main/img/Hydroponic%20Connected.png)
