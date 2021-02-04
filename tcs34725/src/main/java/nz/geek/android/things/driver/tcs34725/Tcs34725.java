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
package nz.geek.android.things.driver.tcs34725;

import android.hardware.Sensor;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.Nullable;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.sensor.UserSensor;
import com.google.android.things.userdriver.sensor.UserSensorDriver;
import com.google.android.things.userdriver.sensor.UserSensorReading;

import java.io.IOException;
import java.util.List;

public class Tcs34725 implements Runnable, AutoCloseable {
  private static final String TAG = Tcs34725.class.getSimpleName();

  private static final int DEVICE_ADDRESS = 0x29;

  private static final int UPDATE_PERIOD = 200;

  /* TCS34725 Registers */
  private static final int COMMAND = 0x80;  //   −− COMMAND W Specifies register address 0x00
  /* package */ static final int ENABLE = (COMMAND | 0x00);   // 0x00 ENABLE R/W Enables states and interrupts 0x00
  /* package */ static final int ATIME = (COMMAND | 0x01);    // 0x01 ATIME R/W RGBC time 0xFF
  /* package */ static final int WTIME = (COMMAND | 0x03);    // 0x03 WTIME R/W Wait time 0xFF
  /* package */ static final int AILTL = (COMMAND | 0x04);    // 0x04 AILTL R/W Clear interrupt low threshold low byte 0x00
  private static final int AILTH = (COMMAND | 0x05);    // 0x05 AILTH R/W Clear interrupt low threshold high byte 0x00
  private static final int AIHTL = (COMMAND | 0x06);    // 0x06 AIHTL R/W Clear interrupt high threshold low byte 0x00
  private static final int AIHTH = (COMMAND | 0x07);    // 0x07 AIHTH R/W Clear interrupt high threshold high byte 0x00
  /* package */ static final int PERS = (COMMAND | 0x0C);     // 0x0C PERS R/W Interrupt persistence filter 0x00
  /* package */ static final int CONFIG = (COMMAND | 0x0D);   // 0x0D CONFIG R/W Configuration 0x00
  /* package */ static final int CONTROL = (COMMAND | 0x0F);  // 0x0F CONTROL R/W Control 0x00
  /* package */ static final int ID = (COMMAND | 0x12);       // 0x12 ID R Device ID ID
  /* package */ static final int STATUS = (COMMAND | 0x13);   // 0x13 STATUS R Device status 0x00
  /* package */ static final int CDATAL = (COMMAND | 0x14);   // 0x14 CDATAL R Clear data low byte 0x00
  private static final int CDATAH = (COMMAND | 0x15);   // 0x15 CDATAH R Clear data high byte 0x00
  private static final int RDATAL = (COMMAND | 0x16);   // 0x16 RDATAL R Red data low byte 0x00
  private static final int RDATAH = (COMMAND | 0x17);   // 0x17 RDATAH R Red data high byte 0x00
  private static final int GDATAL = (COMMAND | 0x18);   // 0x18 GDATAL R Green data low byte 0x00
  private static final int GDATAH = (COMMAND | 0x19);   // 0x19 GDATAH R Green data high byte 0x00
  private static final int BDATAL = (COMMAND | 0x1A);   // 0x1A BDATAL R Blue data low byte 0x00
  private static final int BDATAH = (COMMAND | 0x1B);   // 0x1B BDATAH R Blue data high byte 0x00

  /* COMMAND register values */
  private static final int BYTE_PROTOCOL = 0x00;    // Byte protocol will repeatedly read the same register with each data access.
  /* package */ static final int BLOCK_PROTOCOL = 0x40;   // Block protocol will provide auto-increment function to read successive bytes.
  private static final int CLEAR_INTERRUPT = 0x66;  // Clear channel interrupt clear

  /* ENABLE register values */
  /* package */ static final int AIEN = 0x10; // RGBC interrupt enable. When asserted, permits RGBC interrupts to be generated.
  /* package */ static final int WEN = 0x08;  // Wait enable. Writing a 1 activates the wait timer. Writing a 0 disables the wait timer.
  /* package */ static final int AEN = 0x02;  // RGBC enable. Writing a 1 activates the RGBC. Writing a 0 disables the RGBC.
  /* package */ static final int PON = 0x01;  // Power ON. This bit activates the internal oscillator to permit the timers and ADC channels to operate.
                                        // Writing a 1 activates the oscillator. Writing a 0 disables the oscillator.

  /* CONFIG register values */
  private static final int WLONG = 0x02;  // Wait Long. When asserted, the wait cycles are increased by a factor 12× from that programmed in the
                                          // WTIME register
  
  /* CONTROL register values */
  public static final int GAIN_1 = 0x00;
  public static final int GAIN_4 = 0x01;
  public static final int GAIN_16 = 0x02;
  public static final int GAIN_60 = 0x03;

  /* STATUS register values */
  private static final int AINT = 0x10;   // RGBC clear channel Interrupt.
  private static final int AVALID = 0x01; // RGBC Valid. Indicates that the RGBC channels have completed an integration cycle.

  private static final String DEFAULT_BUS = "I2C1";

  protected final I2cDevice device;

  /**
   * A listener to be notified of sensor readings
   */
  public interface Listener {
    void onColourUpdated(int clear, int red, int green, int blue);
  }

  private Listener listener;
  private HandlerThread handlerThread;
  private Handler handler;
  private UserSensor luxSensor;
  private LuxSensorDriver luxSensorDriver;

  private class LuxSensorDriver implements UserSensorDriver {

    private float lux = 0.0f;
    public void setLux(float lux) {
      this.lux = lux;
    }

    @Override
    public UserSensorReading read() throws IOException {
      return new UserSensorReading(new float[]{lux});
    }
  }

  /* package */ Tcs34725(I2cDevice device) {
    this.device = device;
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
    if (handler != null) {
      handler.removeCallbacks(this);
      handler = null;
    }
    if (handlerThread != null) {
      handlerThread.quitSafely();
      handlerThread = null;
    }
    if (luxSensor != null) {
      UserDriverManager manager = UserDriverManager.getInstance();
      manager.unregisterSensor(luxSensor);
      luxSensor = null;
    }
    if (device != null) {
      try {
        device.close();
      } catch (IOException ignore) {

      }
    }
  }

  private void init() {
    luxSensorDriver = new LuxSensorDriver();
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
   * Create a {@link Tcs34725} on the first I2C bus returned by {@link PeripheralManager#getI2cBusList()}.
   * To specify I2C bus use {@link Tcs34725#create(String)}
   * @return newly created {@link Tcs34725}
   */
  public static Tcs34725 create() {
    return create(getBus());
  }

  /**
   * Create a {@link Tcs34725} on the given I2C bus.
   * @param bus I2C bus, one of the Strings returned by {@link PeripheralManager#getI2cBusList()}
   * @return newly created {@link Tcs34725}
   */
  public static Tcs34725 create(String bus) {
    Tcs34725 tcs34725 = new Tcs34725(getDevice(bus, DEVICE_ADDRESS));
    tcs34725.init();
    return tcs34725;
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setGain(int gain) {
    if (gain >= GAIN_1 && gain <= GAIN_60) {
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
    byte[] buffer = new byte[]{(byte) (lower & 0xFF), (byte) ((lower >> 8) & 0xFF),
            (byte) (upper & 0xFF), (byte) ((upper >> 8) & 0xFF)};
    try {
      device.writeRegBuffer((BLOCK_PROTOCOL | AILTL), buffer, buffer.length);
    } catch (IOException e) {
      //
    }
  }

  /**
   * <pre>
   * value  number of out of range readings causing interrupt
   * --------------------------------------------------------
   * 0      Every RGBC cycle generates an interrupt
   * 1      1
   * 2      2
   * 3      3
   * 4      5
   * 5      10
   * 6      15
   * 7      20
   * 8      25
   * 9      30
   * 10     35
   * 11     40
   * 12     45
   * 13     50
   * 14     55
   * 15     60
   * </pre>
   * @param persistence [0:15]
   */
  public void setInterruptPersistence(int persistence) {
    if (persistence >= 0 && persistence < 16) {
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

  public void registerSensorDriver() {
    UserDriverManager manager = UserDriverManager.getInstance();
    luxSensor = getUserSensor();
    manager.registerSensor(luxSensor);
  }

  public UserSensor getUserSensor() {
    UserSensor.Builder builder = new UserSensor.Builder();
    return builder
            .setName("tcs3472")
            .setVendor("TAOS")
            .setType(Sensor.TYPE_LIGHT)
            .setDriver(luxSensorDriver)
            .build();
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

  private Colour readColour() {
    try {
      byte[] buffer = new byte[8];
      device.readRegBuffer((BLOCK_PROTOCOL | CDATAL), buffer, buffer.length);
      return Colour.fromByteArray(buffer);
    } catch (IOException e) {
      return null;
    }
  }

  private void updateLuxDriver(Colour colour) {
      if (luxSensorDriver != null && colour != null) {
        luxSensorDriver.setLux(colour.toLux());
      }
  }

  private void notifyListener(Colour colour) {
    if (listener != null && colour != null) {
      listener.onColourUpdated(colour.clear, colour.red, colour.green, colour.blue);
    }
  }

  @Override
  public void run() {
    Colour colour = readColour();
    updateLuxDriver(colour);
    notifyListener(colour);
    handler.postDelayed(this, UPDATE_PERIOD);
  }
}
