/*
 * Copyright 2016 Dave McKelvie <www.android.geek.nz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.geek.android.things.drivers.i2c;

import android.support.annotation.Nullable;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;

public abstract class BaseI2cDevice {

  protected final I2cDevice device;

  protected BaseI2cDevice(I2cDevice device) {
    this.device = device;
  }

  protected static String getBus() {
    PeripheralManagerService peripheralManagerService = new PeripheralManagerService();
    List<String> deviceList = peripheralManagerService.getI2cBusList();
    String bus;
    if (deviceList.isEmpty()) {
      bus = "I2C1";
    } else {
      bus = deviceList.get(0);
    }
    return bus;
  }

  @Nullable
  protected static I2cDevice getDevice(String bus, int address) {
    PeripheralManagerService peripheralManagerService = new PeripheralManagerService();
    I2cDevice device = null;
    try {
      device = peripheralManagerService.openI2cDevice(bus, address);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return device;
  }

  protected void close() {
    if (device != null) {
      try {
        device.close();
      } catch (IOException e) {
        // Boo!
      }
    }
  }
}
