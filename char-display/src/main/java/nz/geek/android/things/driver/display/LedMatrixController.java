package nz.geek.android.things.driver.display;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.List;

public class LedMatrixController implements AutoCloseable {

  private static final String DEFAULT_BUS = "SPI0.0";

  private final SpiDevice device;

  public LedMatrixController(SpiDevice device) {
    this.device = device;
  }

  public static LedMatrixController create() {
    return new LedMatrixController(getDevice(getBus()));
  }

  public static LedMatrixController create(String bus) {
    return new LedMatrixController(getDevice(bus));
  }

  protected static String getBus() {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();
    List<String> deviceList = peripheralManager.getSpiBusList();
    if (deviceList.isEmpty()) {
      return DEFAULT_BUS;
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
