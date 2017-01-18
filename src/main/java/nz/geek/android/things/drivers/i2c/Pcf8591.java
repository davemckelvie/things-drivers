/*
 * Copyright 2017 Dave McKelvie
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

/**
 * Android Things driver for the PCF8591 Analog to Digital Converter
 * http://www.nxp.com/documents/data_sheet/PCF8591.pdf
 */
public class Pcf8591 extends BaseI2cDevice {

  /**
   * Device base address
   */
  private static final int BASE_ADDRESS = 0x48;

  /**
   * control byte to be written to device to configure features
   */
  private int control;

  /**
   * Device control byte values
   */
  public static final int ANALOG_OUTPUT_ENABLE = 0x40;
  public static final int MODE_FOUR_SINGLE_ENDED = 0x00;
  public static final int MODE_THREE_DIFFERENTIAL = 0x10;
  public static final int MODE_TWO_SINGLE_ONE_DIFFERENTIAL = 0x20;
  public static final int MODE_TWO_DIFFERENTIAL = 0x30;
  public static final int AUTO_INCREMENT = 0x04;

  /**
   * Constructor given I2cDevice for testing with mock device
   * @param device I2cDevice of the ADC
   */
  /* package */ Pcf8591(I2cDevice device) {
    super(device);
  }

  /**
   * Create a Pcf8591 with the given address on the
   * default I2C bus.
   * @param address value of A0-A2 for your Pcf8591
   * @return new Pcf8591
   */
  public static Pcf8591 create(int address) {
    return create(address, getBus());
  }

  /**
   * Create a Pcf8591 with the given address on the
   * given bus.
   * @param address value of A0-A2 for your Pcf8591
   * @param bus the I2C bus the device is on
   * @return new Pcf8591
   */
  public static Pcf8591 create(int address, String bus) {
    int fullAddress = BASE_ADDRESS + address;
    return new Pcf8591(getDevice(bus, fullAddress));
  }

  public void close() {
    super.close();
  }

  /**
   * set the config value that will be written to the PCF8591
   * @param configuration device configuration, refer datasheet
   */
  public void configure(int configuration) {
    control = configuration;
  }

  /**
   * read a single ADC channel
   * @param channel to read [0:3]
   * @return ADC result
   */
  public int readChannel(int channel) {
    if (channel < 0 || channel > 3) return -1;

    byte[] config = {(byte) ((channel | control) & 0xFF)};
    byte[] buffer = new byte[2];
    try {
      device.write(config, 1);
      device.read(buffer, buffer.length);
    } catch (IOException e) {
      // nah, bra
    }
    return (buffer[1] & 0xFF);
  }

  /**
   * Read all ADC channels
   * @return values for channels 0 - 3
   */
  public int[] readAllChannels() {
    byte[] config = {(byte) (control | AUTO_INCREMENT)};
    byte[] buffer = new byte[5];
    try {
      device.write(config, 1);
      device.read(buffer, buffer.length);
    } catch (IOException e) {
      // nope
    }
    return new int[]{buffer[1] & 0xFF, buffer[2] & 0xFF, buffer[3] & 0xFF, buffer[4] & 0xFF};
  }
}
