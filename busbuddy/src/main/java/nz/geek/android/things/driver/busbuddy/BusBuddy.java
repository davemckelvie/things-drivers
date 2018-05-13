package nz.geek.android.things.driver.busbuddy;


import android.support.annotation.Nullable;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.List;

public class BusBuddy implements AutoCloseable {

  /* package */ BusBuddy(I2cDevice device) {
    this.device = device;
  }

  public static BusBuddy create(int address) {
    return new BusBuddy(getDevice(getBus(), address));
  }

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
  private static final String DEFAULT_BUS = "I2C1";

  private final I2cDevice device;

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
}