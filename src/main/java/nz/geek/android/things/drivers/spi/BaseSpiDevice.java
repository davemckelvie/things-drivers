package nz.geek.android.things.drivers.spi;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.List;

public class BaseSpiDevice {

  protected final SpiDevice device;

  public BaseSpiDevice(SpiDevice device) {
    this.device = device;
  }

  protected static String getBus() {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();
    List<String> deviceList = peripheralManager.getSpiBusList();
    if (deviceList.isEmpty()) {
      return "SPI0.0";
    } else {
      return deviceList.get(0);
    }
  }

  protected static SpiDevice getDevice(String bus) {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();

    try {
      SpiDevice spiDevice = peripheralManager.openSpiDevice(bus);
      spiDevice.setMode(SpiDevice.MODE0);
      spiDevice.setBitJustification(false);
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
      } catch (IOException e) {
        //
      }
    }
  }

  public void close() {
    if (device != null) {
      try {
        device.close();
      } catch (IOException e) {
        // Boo!
      }
    }
  }
}
