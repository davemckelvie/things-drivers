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

import android.os.Handler;
import android.os.HandlerThread;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import nz.geek.android.things.drivers.colour.ColourSensor;

public class Tcs34725 extends BaseI2cDevice implements Runnable {
  private static final String TAG = Tcs34725.class.getSimpleName();

  private static final int BASE_ADDRESS = 0x29;

  private static final int UPDATE_PERIOD = 2000;

  /* TCS34725 Registers */
  private static final int COMMAND = 0x80;  //   −− COMMAND W Specifies register address 0x00
  private static final int ENABLE = (COMMAND | 0x00);   // 0x00 ENABLE R/W Enables states and interrupts 0x00
  private static final int ATIME = (COMMAND | 0x01);    // 0x01 ATIME R/W RGBC time 0xFF
  private static final int WTIME = (COMMAND | 0x03);    // 0x03 WTIME R/W Wait time 0xFF
  private static final int AILTL = (COMMAND | 0x04);    // 0x04 AILTL R/W Clear interrupt low threshold low byte 0x00
  private static final int AILTH = (COMMAND | 0x05);    // 0x05 AILTH R/W Clear interrupt low threshold high byte 0x00
  private static final int AIHTL = (COMMAND | 0x06);    // 0x06 AIHTL R/W Clear interrupt high threshold low byte 0x00
  private static final int AIHTH = (COMMAND | 0x07);    // 0x07 AIHTH R/W Clear interrupt high threshold high byte 0x00
  private static final int PERS = (COMMAND | 0x0C);     // 0x0C PERS R/W Interrupt persistence filter 0x00
  private static final int CONFIG = (COMMAND | 0x0D);   // 0x0D CONFIG R/W Configuration 0x00
  private static final int CONTROL = (COMMAND | 0x0F);  // 0x0F CONTROL R/W Control 0x00
  private static final int ID = (COMMAND | 0x12);       // 0x12 ID R Device ID ID
  private static final int STATUS = (COMMAND | 0x13);   // 0x13 STATUS R Device status 0x00
  private static final int CDATAL = (COMMAND | 0x14);   // 0x14 CDATAL R Clear data low byte 0x00
  private static final int CDATAH = (COMMAND | 0x15);   // 0x15 CDATAH R Clear data high byte 0x00
  private static final int RDATAL = (COMMAND | 0x16);   // 0x16 RDATAL R Red data low byte 0x00
  private static final int RDATAH = (COMMAND | 0x17);   // 0x17 RDATAH R Red data high byte 0x00
  private static final int GDATAL = (COMMAND | 0x18);   // 0x18 GDATAL R Green data low byte 0x00
  private static final int GDATAH = (COMMAND | 0x19);   // 0x19 GDATAH R Green data high byte 0x00
  private static final int BDATAL = (COMMAND | 0x1A);   // 0x1A BDATAL R Blue data low byte 0x00
  private static final int BDATAH = (COMMAND | 0x1B);   // 0x1B BDATAH R Blue data high byte 0x00

  /* COMMAND register values */
  private static final int BYTE_PROTOCOL = 0x00;    // Byte protocol will repeatedly read the same register with each data access.
  private static final int BLOCK_PROTOCOL = 0x40;   // Block protocol will provide auto-increment function to read successive bytes.
  private static final int CLEAR_INTERRUPT = 0x66;  // Clear channel interrupt clear

  /* ENABLE register values */
  private static final int AIEN = 0x10; // RGBC interrupt enable. When asserted, permits RGBC interrupts to be generated.
  private static final int WEN = 0x08;  // Wait enable. Writing a 1 activates the wait timer. Writing a 0 disables the wait timer.
  private static final int AEN = 0x02;  // RGBC enable. Writing a 1 activates the RGBC. Writing a 0 disables the RGBC.
  private static final int PON = 0x01;  // Power ON. This bit activates the internal oscillator to permit the timers and ADC channels to operate.
                                        // Writing a 1 activates the oscillator. Writing a 0 disables the oscillator.

  /* CONFIG register values */
  private static final int WLONG = 0x02;  // Wait Long. When asserted, the wait cycles are increased by a factor 12× from that programmed in the
                                          // WTIME register
  
  /* CONTROL register values */
  private static final int GAIN_1 = 0x00;
  private static final int GAIN_4 = 0x01;
  private static final int GAIN_16 = 0x02;
  private static final int GAIN_60 = 0x03;

  /* STATUS register values */
  private static final int AINT = 0x10;   // RGBC clear channel Interrupt.
  private static final int AVALID = 0x01; // RGBC Valid. Indicates that the RGBC channels have completed an integration cycle.

  private ColourSensor.Listener listener;
  private HandlerThread handlerThread;
  private Handler handler;

  /* package */ Tcs34725(I2cDevice device, int address) {
    super(device, address);
    initHandler();
  }

  private void initHandler() {
    if (handlerThread == null) {
      handlerThread = new HandlerThread(TAG);
      handlerThread.start();
    }
    if (handler == null) {
      handler = new Handler(handlerThread.getLooper());
    }
  }

  /**
   * Create a {@link Tcs34725} on the first I2C bus returned by {@link PeripheralManagerService#getI2cBusList()}.
   * To specify I2C bus use {@link Tcs34725#create(String)}
   * @return newly created {@link Tcs34725}
   */
  public static Tcs34725 create() {
    return create(getBus());
  }

  /**
   * Create a {@link Tcs34725} on the given I2C bus.
   * @param bus I2C bus, one of the Strings returned by {@link PeripheralManagerService#getI2cBusList()}
   * @return newly created {@link Tcs34725}
   */
  public static Tcs34725 create(String bus) {
    int fullAddress = BASE_ADDRESS;
    I2cDevice device = getDevice(bus, fullAddress);
    return new Tcs34725(device, fullAddress);
  }

  /**
   * Close sensor and tidy up treads. After a call to {@link #close()} the {@link Tcs34725} is invalid
   * and should not be used.
   */
  public void close() {
    super.close();
    if (handler != null) {
      handler.removeCallbacks(this);
      handler = null;
    }
    if (handlerThread != null) {
      handlerThread.quitSafely();
      handlerThread = null;
    }
  }

  public void setListener(ColourSensor.Listener listener) {
    this.listener = listener;
  }

  public void setGain(int gain) {
    if (gain > 0 && gain < 4) {
      writeRegister(CONTROL, gain);
    }
  }

  public void setIntegrationTime(float time) {
    if (time < 2.4f || time > 614f) return; // out of range
    int atime = (int) (256 - (time / 2.4));
    writeRegister(ATIME, atime);
  }

  // TODO: set longer times (WLONG)
  public void setWaitTime(float time) {
    if (time < 2.4f || time > 614f) return; // out of range
    int atime = (int) (256 - (time / 2.4));
    writeRegister(WTIME, atime);
  }

  public void setInterruptThresholds(int lower, int upper) {
    byte[] buffer = new byte[]{(byte) (lower & 0xFF), (byte) (lower >> 4), (byte) (upper & 0xFF), (byte) (upper >> 4)};
    try {
      device.writeRegBuffer((BLOCK_PROTOCOL | AILTL), buffer, buffer.length);
    } catch (IOException e) {
      //
    }
  }

  /**
   * @param persistence [0:15]
   * 0000 Every RGBC cycle generates an interrupt
   * 0001 1 clear channel value outside of threshold range
   * 0010 2 clear channel consecutive values out of range
   * 0011 3 clear channel consecutive values out of range
   * 0100 5 clear channel consecutive values out of range
   * 0101 10 clear channel consecutive values out of range
   * 0110 15 clear channel consecutive values out of range
   * 0111 20 clear channel consecutive values out of range
   * 1000 25 clear channel consecutive values out of range
   * 1001 30 clear channel consecutive values out of range
   * 1010 35 clear channel consecutive values out of range
   * 1011 40 clear channel consecutive values out of range
   * 1100 45 clear channel consecutive values out of range
   * 1101 50 clear channel consecutive values out of range
   * 1110 55 clear channel consecutive values out of range
   * 1111 60 clear channel consecutive values out of range
   */
  public void setInterruptPersistence(int persistence) {
    if (persistence > 0 && persistence < 16) {
      writeRegister(PERS, persistence);
    }
  }

  public void enableInterrupt(boolean enable) {
    int enableRegister = readRegister(ENABLE);
    int en = enable ? (enableRegister | AIEN) : (enableRegister & ~AIEN);
    writeRegister(ENABLE, en);
  }

  public void enableWaitTime(boolean enable) {
    int enableRegister = readRegister(ENABLE);
    int en = enable ? (enableRegister | WEN) : (enableRegister & ~WEN);
    writeRegister(ENABLE, en);
  }

  public void enable(boolean enable) {
    initHandler();
    int enableRegister = readRegister(ENABLE);
    int en = enable ? (enableRegister | PON | AEN) : (enableRegister & ~(PON | AEN));
    writeRegister(ENABLE, en);
    if (enable) {
      handler.postDelayed(this, UPDATE_PERIOD);
    } else {
      handler.removeCallbacks(this);
    }
  }

  public int readStatus() {
    return readRegister(STATUS);
  }

  public int readId() {
    return readRegister(ID);
  }

  private int readRegister(int reg) {
    try {
      return device.readRegByte(reg);
    } catch (IOException e) {
      //
    }
    return -1;
  }

  private void writeRegister(int reg, int data) {
    try {
      device.writeRegByte(reg, (byte) (data & 0xFF));
    } catch (IOException e) {
      //
    }
  }

  private void readColour() {
    try {
      byte[] buffer = new byte[8];
      device.readRegBuffer((BLOCK_PROTOCOL | CDATAL), buffer, buffer.length);
      notifyListener(buffer);
    } catch (IOException e) {
      //
    }
  }

  private void notifyListener(byte[] data) {
    if (listener != null && data.length >= 8) {
      int clear = (data[0] & 0xFF) | (data[1] << 8);
      int red   = (data[2] & 0xFF) | (data[3] << 8);
      int green = (data[4] & 0xFF) | (data[5] << 8);
      int blue  = (data[6] & 0xFF) | (data[7] << 8);
      listener.onColourUpdated(clear, red, green, blue);
    }
  }

  @Override
  public void run() {
    readColour();
    handler.postDelayed(this, UPDATE_PERIOD);
  }
}
