/*
 * Copyright 2018 Dave McKelvie <www.android.geek.nz>.
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
 */package nz.geek.android.things.driver.display;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.List;

public class BaseSpiDevice implements AutoCloseable{

  protected final SpiDevice device;

  protected BaseSpiDevice(SpiDevice device) {
    this.device = device;
  }

  protected static String getBus() {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();
    List<String> deviceList = peripheralManager.getSpiBusList();
    if (deviceList.isEmpty()) {
      return "SPI0.0";
    } else {
      return deviceList.get(0);
    }
  }

  protected static SpiDevice getDevice(String bus) {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();

    try {
      SpiDevice spiDevice = peripheralManager.openSpiDevice(bus);
      spiDevice.setMode(SpiDevice.MODE0);
      spiDevice.setBitJustification(SpiDevice.BIT_JUSTIFICATION_MSB_FIRST);
      spiDevice.setBitsPerWord(8);
      spiDevice.setFrequency(4500000);
      return spiDevice;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void write(byte[] data) {
    if (device != null) {
      try {
        device.write(data, data.length);
      } catch (IOException ignore) {
        //
      }
    }
  }

  @Override
  public void close() {
    if (device != null) {
      try {
        device.close();
      } catch (IOException ignore) {
        //
      }
    }
  }
}
