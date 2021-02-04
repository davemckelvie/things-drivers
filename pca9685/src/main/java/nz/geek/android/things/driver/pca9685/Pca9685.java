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
 */
package nz.geek.android.things.driver.pca9685;


import androidx.annotation.Nullable;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.List;

/**
 * Android Things driver for the PCA9685 16 channel, 12 bit PWM controller
 * https://www.nxp.com/docs/en/data-sheet/PCA9685.pdf
 */
public class Pca9685 implements AutoCloseable {

  /**
   * Device base address
   */
  private static final int BASE_ADDRESS = 0x40;

  private static final int MIN_FREQ = 24;
  private static final int MAX_FREQ = 1526;
  private static final int CLK_FREQ = 25000000;
  private static final int NUM_PINS = 16;
  private static final double MIN_DUTY_CYCLE = 0.0;
  private static final double MAX_DUTY_CYCLE = 100.0;

  /**
   * MODE 1 register definitions
   */
  private static final int MODE_1_ADDR = 0x00;
  private static final int ALLCALL     = 0;
  private static final int SUB3        = 1;
  private static final int SUB2        = 2;
  private static final int SUB1        = 3;
  private static final int SLEEP       = 4;
  private static final int AI          = 5;
  private static final int EXTCLK      = 6;
  private static final int RESTART     = 7;

  /**
   * MODE2 register definitions
   */
  private static final int MODE_2_ADDR = 0x01;
  private static final int OUTNE0      = 0;
  private static final int OUTNE1      = 1;
  private static final int OUTDRV      = 2;
  private static final int OCH         = 3;
  private static final int INVRT       = 4;

  /**
   * Other register addresses
   */
  private static final int SUBADR1       = 0x02;
  private static final int SUBADR2       = 0x03;
  private static final int SUBADR3       = 0x04;
  private static final int ALLCALLADR    = 0x05;
  private static final int LED0_ON_L     = 0x06;
  private static final int LED0_ON_H     = 0x07;
  private static final int LED0_OFF_L    = 0x08;
  private static final int LED0_OFF_H    = 0x09;
  private static final int ALL_LED_ON_L  = 0xFA;
  private static final int ALL_LED_ON_H  = 0xFB;
  private static final int ALL_LED_OFF_L = 0xFC;
  private static final int ALL_LED_OFF_H = 0xFD;
  private static final int PRE_SCALE     = 0xFE;
  private static final int TEST_MODE     = 0xFF;

  private static final String DEFAULT_BUS = "I2C1";

  private final I2cDevice device;


  /**
   * Constructor given I2cDevice for testing with mock device
   * @param device I2cDevice of the ADC
   */
  /* package */ Pca9685(I2cDevice device) {
    this.device = device;
  }

  /**
   * Create a Pca9685 with the given address on the
   * default I2C bus.
   * @param address value of A0-A4 for your Pca9685
   * @return new Pca9685
   */
  public static Pca9685 create(int address) {
    return create(address, getBus());
  }

  /**
   * Create a Pca9685 with the given address on the
   * given bus.
   * @param address value of A0-A4 for your Pca9685
   * @param bus the I2C bus the device is on
   * @return new Pca9685
   */
  public static Pca9685 create(int address, String bus) {
    int fullAddress = BASE_ADDRESS + address;
    return new Pca9685(getDevice(bus, fullAddress));
  }

  /**
   * Convenience method to get an I2C bus
   * @return the first bus or a default bus
   */
  protected static String getBus() {
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
  protected static I2cDevice getDevice(String bus, int address) {
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
   * Set the PWM frequency of the device. This can only be set on the device level,
   * not individual pins, it is set to the last set value for any of the 16 pins
   * @param frequency required frequency in Hz
   * @throws IllegalArgumentException if frequency out of range
   * @throws IOException on I2C exception
   */
  public void setPwmFrequencyHz(double frequency) throws IllegalArgumentException, IOException {
    if (frequency < MIN_FREQ || frequency > MAX_FREQ) {
      throw new IllegalArgumentException("frequency out of range (" + MIN_FREQ + "-" + MAX_FREQ + ")");
    }

    // from datasheet (7.3.5)
    int preScale = Math.round((float) (CLK_FREQ / (4096 * frequency))) - 1;

    int mode1 = device.readRegByte(MODE_1_ADDR);

    // put to sleep
    int newMode = mode1 | BV(SLEEP);
    device.writeRegByte(MODE_1_ADDR, (byte)(newMode & 0xFF));

    // write preScale value
    device.writeRegByte(PRE_SCALE, (byte)(preScale & 0xFF));

    // wake up, auto increment
    newMode = BV(AI);
    device.writeRegByte(MODE_1_ADDR, (byte)(newMode & 0xFF));
  }

  /**
   * Set the PWM duty cycle of the given pin
   * @param dutyCycle PWM duty cycle [0:100]%
   * @param pin PWM output pin [0:15]
   * @throws IllegalArgumentException if duty cycle or pin out of range
   * @throws IOException on I2C exception
   */
  public void setPwmDutyCycle(double dutyCycle, int pin) throws IllegalArgumentException, IOException {
    if (dutyCycle < MIN_DUTY_CYCLE || dutyCycle > MAX_DUTY_CYCLE) throw new IllegalArgumentException("duty cycle out of range");

    int offTime = (int)Math.floor(4095 / 100 * dutyCycle);

    setPinPwmOnOff(0, offTime, pin);
  }

  /**
   * Set the given PWM pin on and off times
   * @param on on time [0:4095]
   * @param off off time [0:4095]
   * @param pin PWM output pin [0:15]
   * @throws IllegalArgumentException if on and off times are the same
   * @throws IOException on I2C exception
   */
  public void setPinPwmOnOff(int on, int off, int pin) throws IllegalArgumentException, IOException {
    if (on == off) throw new IllegalArgumentException("on and off time cannot be the same");
    if (pin < 0 || pin > 15) throw new IllegalArgumentException("pin out of range");

    byte[] buffer = new byte[4];

    buffer[0] = (byte) (on & 0xFF);
    buffer[1] = (byte) ((on >> 8) & 0xFF);

    buffer[2] = (byte) (off & 0xFF);
    buffer[3] = (byte) ((off >> 8) & 0xFF);

    device.writeRegBuffer((LED0_ON_L + (4 * pin)), buffer, buffer.length);
  }

  /**
   * disable PWM and set given pin high or low depending on given 'state'
   * @param state set pin high when true
   * @param pin output pin [0:15]
   * @throws IOException on I2C exception
   */
  public void setPin(boolean state, int pin) throws IOException {
    if (state) {
      setPinPwmOnOff(4096, 0, pin);
    } else {
      setPinPwmOnOff(0, 4096, pin);
    }
  }
}
