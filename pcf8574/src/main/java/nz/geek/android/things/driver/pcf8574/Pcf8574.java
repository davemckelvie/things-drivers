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
package nz.geek.android.things.driver.pcf8574;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.List;

/**
 * Android Things driver for the PCF8574 8 bit I2C IO port
 * https://www.nxp.com/documents/data_sheet/PCF8574.pdf
 */
public class Pcf8574 implements AutoCloseable {

  /**
   * device base address for PCF8574A
   */
  private static final int BASE_ADDRESS = 0x38;

  /**
   * device base address for PCF8574
   */
  private static final int BASE_ADDRESS_PCF8574 = 0x20;

  private static final String DEFAULT_BUS = "I2C1";

  private final I2cDevice device;

  /**
   * because the PCF8574 is a 'quasi-bidirectional' any value written
   * will be changed by a read, so cache the values written to keep
   * track of the current state of the port pins.
   */
  private int currentValue = 0x00;

  /**
   * Constructor given I2cDevice for testing with mock device
   * @param device I2cDevice of the port
   */
  @VisibleForTesting
  /* package */ Pcf8574(I2cDevice device) {
    this.device = device;
  }

  /**
   * Create a pcf8574 with the given address
   * @param address value of A0-A2 for your pcf8574
   * @param isPcf8574 is a PCF8574 (not a PCF8574A)
   * @return new Pcf8574
   */
  public static Pcf8574 create(int address, boolean isPcf8574) {
    return create(address, getBus(), isPcf8574);
  }

  /**
   * Create a pcf8574 with the given address on the
   * given bus.
   * @param address value of A0-A2 for your Pcf8574
   * @param bus the I2C bus the device is on
   * @param isPcf8574 is a PCF8574 (not a PCF8574A)
   * @return new Pcf8574
   */
  public static Pcf8574 create(int address, String bus, boolean isPcf8574) {
    int fullAddress = (isPcf8574 ? BASE_ADDRESS_PCF8574 : BASE_ADDRESS) + address;
    return new Pcf8574(getDevice(bus, fullAddress));
  }

  /**
   * Convenience method to get an I2C bus
   * @return the first bus or a default bus
   */
  private static String getBus() {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();
    List<String> deviceList = peripheralManager.getI2cBusList();
    if (deviceList.isEmpty()) {
      return DEFAULT_BUS;
    } else {
      return deviceList.get(0);
    }
  }

  /**
   * Convenience method to get an I2cDevice on the given bus with the given I2C address
   * @param bus the bus that the device is connected to
   * @param address the address of the device
   * @return the opened device or null
   */
  @Nullable
  private static I2cDevice getDevice(String bus, int address) {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();

    try {
      return peripheralManager.openI2cDevice(bus, address);
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }

  /**
   * return the bit value of pin number
   * @param pin number [0:7]
   * @return bit value [0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80] etc
   */
  public static int BV(int pin) {
    return (1 << pin);
  }

  @Override
  public void close() {
    if (device != null) {
      try {
        device.close();
      } catch (IOException ignore) {

      }
    }
  }

  /**
   * Write a byte to the IO port applying a mask to the data. This enables the
   * setting of port state while not affecting the state of other port pins.
   * @param mask 8 bit mask, data does not affect port state when mask bit is 1
   * @param data The data to write to the port
   * @return true when data written to port
   */
  public boolean writeByte(int mask, int data) {
    if (device == null) return false;

    int value = currentValue;

    data &= ~mask;  // apply the mask to the data
    value &= mask;  // apply the mask to the current value
    value |= data;  // apply the data to the current value
    value &= 0xFF;  // clear any higher bits

    byte[] buffer = new byte[1];
    buffer[0] = (byte) (value & 0xFF);
    try {
      device.write(buffer, buffer.length);
      currentValue = value;
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Read the last value written to the port.
   * @return last value written to the port (LSB is value)
   */
  public int readValue() {
    return (currentValue & 0xFF);
  }

  /**
   * set the given port pin to the given value
   * @param pin the pin to set [0:7]
   * @param state true to set it, false to clear it
   */
  public void setPin(int pin, boolean state) {
    if (pin < 0 || pin > 7) return;
    int mask = ~BV(pin);
    int data = state ? BV(pin) : 0;
    writeByte(mask, data & 0xFF);
  }

  /**
   * Get the state of the given port pin
   * @param pin the pin to get [0:7]
   * @return true when set (logic 1), false when clear (logic 0)
   */
  public boolean getPin(int pin) {
    if (pin < 0 || pin > 7) return false;
    int value = readByte();
    return ((value & BV(pin)) == BV(pin));
  }

  /**
   * Read the port data, the state of all pins.
   * @return the byte read from the port (as an int LSB is port data)
   */
  public int readByte() {
    if (device == null) return 0;

    byte[] buffer = new byte[]{0};

    try {
      device.read(buffer, 1);
    } catch (IOException e) {
      // ignore
    }
    return buffer[0];
  }
}
