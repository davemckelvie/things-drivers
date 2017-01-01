package nz.geek.android.things.drivers.i2c;

import com.google.android.things.pio.I2cDevice;

import java.io.IOException;

public class Pcf8591 extends BaseI2cDevice {

  private static final int BASE_ADDRESS = 0x48;
  private int control;
  private int mode;

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
   * @param address base address + value of A0-A2 for your board
   */
  /* package */ Pcf8591(I2cDevice device, int address) {
    super(device, address);
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
    I2cDevice device = getDevice(bus, fullAddress);
    return new Pcf8591(device, fullAddress);
  }

  public void close() {
    super.close();
  }

  public void configure(int configuration) {
    control = configuration;
  }

  public int readChannel(int channel) {
    if (channel < 0 || channel > 3) return -1;

    byte[] config = {(byte) (channel & control)};
    byte[] buffer = new byte[2];
    try {
      device.write(config, 1);
      device.read(buffer, 2);
    } catch (IOException e) {
      // nah, bra
    }
    return (buffer[1] & 0xFF);
  }

}
