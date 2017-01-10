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

import com.google.android.things.pio.I2cDevice;

import java.io.IOException;

import nz.geek.android.things.drivers.IoPort;

/**
 * Android Things driver for the PCF8574 8 bit I2C IO port
 * https://www.nxp.com/documents/data_sheet/PCF8574.pdf
 */
public class Pcf8574 extends BaseI2cDevice implements IoPort {

  /**
   * device base address for PCF8574A
   */
  private static final int BASE_ADDRESS = 0x38;

  /**
   * device base address for PCF8574
   */
  private static final int BASE_ADDRESS_PCF8574 = 0x20;

  /**
   * because the PCF8574 is a 'quasi-bidirectional' any value written
   * will be changed by a read, so cache the values written to keep
   * track of the current state of the port pins.
   */
  private int currentValue = 0x00;

  /**
   * Constructor given I2cDevice for testing with mock device
   * @param device I2cDevice of the port
   * @param address base address + value of A0-A2 for your board
   */
  /* package */ Pcf8574(I2cDevice device, int address) {
    super(device, address);
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
    I2cDevice device = getDevice(bus, fullAddress);
    return new Pcf8574(device, fullAddress);
  }

  /**
   * return the bit value of pin number
   * @param pin number [0:7]
   * @return bit value
   */
  public static int BV(int pin) {
    return (1 << pin);
  }

  /**
   * Write a byte to the IO port applying a mask to the data. This enables the
   * setting of port state while not affecting the state of other port pins.
   * @param mask 8 bit mask, data does not affect port state when mask bit is 1
   * @param data The data to write to the port
   * @return true when data written to port
   */
  @Override
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
   * Read the last value written to the port
   * @return currentValue
   */
  @Override
  public int readValue() {
    return (currentValue & 0xFF);
  }

  @Override
  public void setPin(int pin, boolean state) {
    if (pin > 7) return;
    int mask = ~(1 << pin);
    int data = state ? 1 << pin : 0;
    writeByte(mask, data & 0xFF);
  }

  @Override
  public void close() {
    super.close();
  }

  /**
   * read the port. This may not do what you want because it's 'quasi-bidirectional.'
   * Read the data sheet, it explains it much better.
   * @return
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
