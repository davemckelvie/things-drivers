PCF8574 IO driver for Android Things
====================================

[ ![Download](https://api.bintray.com/packages/davemckelvie/maven/things-driver-pcf8574/images/download.svg) ][latest]

Android Things driver for the [PCF8574](https://www.nxp.com/documents/data_sheet/PCF8574.pdf) 8 bit I2C IO port

How to use the driver
---------------------

### Gradle dependency

To use the `pcf8574` driver, add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][latest].

```
dependencies {
    implementation 'nz.geek.android.things:things-driver-pcf8574:<version>'
}
```

### Hardware hookup

![Raspberry Pi Hookup](rpi3_pcf8574.png)

### Sample usage

```java
import nz.geek.android.things.driver.pcf8574.Pcf8574;

// Access IO port:

Pcf8574 pcf8574;

// create a port with an address (the value of A0-A2), an I2C bus name,
// and a flag to indicate that the port type is a PCF8574 as opposed to
// a PCF8574A (which has a different base address)

pcf8574 = Pcf8574.create(pcf8574Address, i2cBusName, isPcf8574);

// Write to port, the first argument is a bitmask, the second is
// the data to write. Data does not affect port pins where mask bit is 1

pcf8574.writeByte(0x00, 0x00);

// Close the port when finished:

try {
    pcf8574.close();
} catch (IOException e) {
    // error closing IO port
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

[latest]: https://bintray.com/davemckelvie/maven/things-driver-pcf8574/_latestVersion

