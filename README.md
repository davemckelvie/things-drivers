# DaSiAnThiLib (Dave's Simple Android Things Library)

Here you will find some Android Things um, things, drivers and such.

## I2C Things

PCF8574(A)

## Display Things

HD44870 type displays (using those cheap i2c converter modules)

## How to use

1. Clone repo ```git clone https://bitbucket.org/subverse/things-drivers.git```
2. create a new project in Android studio for Android Things
3. import the driver as a module
4. create the lcd object using an `I2cSerialCharLcd.builder()` passing module width and height
5. use the builder to setup the pin mapping between PCF8574 pins and LCD pins
6. call ```lcd.connect()``` to open and initialise the display
7. some time later write something to the display
```java
@Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    I2cSerialCharLcd.builder builder = I2cSerialCharLcd.builder(16, 4);
    builder.rs(0).rw(1).e(2).bl(3).data(4, 5, 6, 7).address(7);
    I2cSerialCharLcd lcd = I2cSerialCharLcd.create(16, 4);
    lcd.connect();
    handler.postDelayed(connectRunnable, 1000);
  }
```
8. write to the display with lcd.print(). The first argument is the LCD line to write to 
```java
private final class ConnectRunnable implements Runnable {

    int count = 0;
    @Override
    public void run() {
      lcd.print(1, "Android Tings <3");
      lcd.print(2, String.valueOf(count++));

      handler.postDelayed(this, 2000);
    }
  }
```

## License

Copyright 2016  Dave McKelvie

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
