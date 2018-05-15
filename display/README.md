Character display driver for Android Things
===========================================

HD44870 type displays (via LCM1602)

![Raspberry Pi Hookup](rpi3_lcd.png)

## Usage

1. Create an Android Things project as described [here](https://developer.android.com/things/training/first-device/create-studio-project.html)
2. Add the following to your project's `build.gradle` replace `version` with [latest version][latest]
```
dependencies {
    implementation 'nz.geek.android.things:things-driver-char-display:<version>'
}
```
3. create the lcd object using an `I2cLcdCharacterDisplay.builder()` passing module width and height
4. use the builder to setup the pin mapping between PCF8574 pins and LCD pins
5. call `lcd.connect()` to open and initialise the display
6. write something to the display
```java

private CharacterDisplay lcd;

@Override
protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);

  // create a display builder with the LCD module width and height
  I2cLcdCharacterDisplay.Builder builder = I2cLcdCharacterDisplay.builder(20, 4);

  // set port pin to LCD pin mapping, and PCF8574(A) address
  builder.rs(0).rw(1).e(2).bl(3).data(4, 5, 6, 7).address(6);

  // build and use the display
  CharacterDisplay lcd = builder.build();
  lcd.connect();

  // write message to the display, the first argument
  // is the LCD line (row) number
  lcd.print(1, "Hello World!");
}

@Override
public void onDestroy() {
  super.onDestroy();

  // disconnect from the display to free resources
  lcd.disconnect();
}
```
License
-------

Copyright 2018 Dave McKelvie.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

[latest][https://bintray.com/davemckelvie/maven/things-driver-char-display/_latestVersion]

