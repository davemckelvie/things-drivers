package nz.geek.android.things.driver.display;

import com.google.android.things.pio.SpiDevice;

public class LedMatrixController extends BaseSpiDevice {

  public LedMatrixController(SpiDevice device) {
    super(device);
  }

  public static LedMatrixController create() {
    return new LedMatrixController(getDevice(getBus()));
  }

  public static LedMatrixController create(String bus) {
    return new LedMatrixController(getDevice(bus));
  }
}
