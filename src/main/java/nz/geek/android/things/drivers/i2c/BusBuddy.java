package nz.geek.android.things.drivers.i2c;


import com.google.android.things.pio.I2cDevice;

import java.io.IOException;

public class BusBuddy extends BaseI2cDevice {

  /* package */ BusBuddy(I2cDevice device) {
    super(device);
  }

  public static BusBuddy create(int address) {
    return new BusBuddy(getDevice(getBus(), address));
  }

  public void destroy() {
    super.close();
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
}