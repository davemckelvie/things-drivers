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
4. create the lcd object passing module width and height to ```create()```
5. call ```lcd.connect()``` to open and initialise the display
6. some time later write something to the display (I'll fix this later)
```java
@Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lcd = I2cSerialCharLcd.create(16, 4);
    lcd.connect();
    handler.postDelayed(connectRunnable, 1000);
  }
```
7. write to the display with lcd.print(). The first argument is the 'position' to start writing from
```java
private final class ConnectRunnable implements Runnable {

    int count = 0;
    @Override
    public void run() {
      lcd.print(I2cSerialCharLcd.LCD_LINE_ONE, "Android Tings <3");
      lcd.print(I2cSerialCharLcd.LCD_LINE_TWO, String.valueOf(count++));

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
