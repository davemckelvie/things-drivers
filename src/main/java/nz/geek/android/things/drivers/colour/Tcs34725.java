package nz.geek.android.things.drivers.colour;

import android.os.Handler;
import android.os.HandlerThread;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;

import java.io.IOException;

import nz.geek.android.things.drivers.i2c.BaseI2cDevice;

public class Tcs34725 extends BaseI2cDevice implements Runnable {
  private static final String TAG = Tcs34725.class.getSimpleName();

  private static final int BASE_ADDRESS = 0x29;

  private static final int UPDATE_PERIOD = 500;

  /* TCS34725 Registers */
  private static final int COMMAND = 0x80;  //   −− COMMAND W Specifies register address 0x00
  private static final int ENABLE = 0x00;   // 0x00 ENABLE R/W Enables states and interrupts 0x00
  private static final int ATIME = 0x01;    // 0x01 ATIME R/W RGBC time 0xFF
  private static final int WTIME = 0x03;    // 0x03 WTIME R/W Wait time 0xFF
  private static final int AILTL = 0x04;    // 0x04 AILTL R/W Clear interrupt low threshold low byte 0x00
  private static final int AILTH = 0x05;    // 0x05 AILTH R/W Clear interrupt low threshold high byte 0x00
  private static final int AIHTL = 0x06;    // 0x06 AIHTL R/W Clear interrupt high threshold low byte 0x00
  private static final int AIHTH = 0x07;    // 0x07 AIHTH R/W Clear interrupt high threshold high byte 0x00
  private static final int PERS = 0x0C;     // 0x0C PERS R/W Interrupt persistence filter 0x00
  private static final int CONFIG = 0x0D;   // 0x0D CONFIG R/W Configuration 0x00
  private static final int CONTROL = 0x0F;  // 0x0F CONTROL R/W Control 0x00
  private static final int ID = 0x12;       // 0x12 ID R Device ID ID
  private static final int STATUS = 0x13;   // 0x13 STATUS R Device status 0x00
  private static final int CDATAL = 0x14;   // 0x14 CDATAL R Clear data low byte 0x00
  private static final int CDATAH = 0x15;   // 0x15 CDATAH R Clear data high byte 0x00
  private static final int RDATAL = 0x16;   // 0x16 RDATAL R Red data low byte 0x00
  private static final int RDATAH = 0x17;   // 0x17 RDATAH R Red data high byte 0x00
  private static final int GDATAL = 0x18;   // 0x18 GDATAL R Green data low byte 0x00
  private static final int GDATAH = 0x19;   // 0x19 GDATAH R Green data high byte 0x00
  private static final int BDATAL = 0x1A;   // 0x1A BDATAL R Blue data low byte 0x00
  private static final int BDATAH = 0x1B;   // 0x1B BDATAH R Blue data high byte 0x00

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

  private Listener listener;
  private HandlerThread handlerThread;
  private Handler handler;
  private Gpio gpio;
  private GpioCallback gpioCallback = new GpioCallback() {
    @Override
    public boolean onGpioEdge(Gpio gpio) {
      return super.onGpioEdge(gpio);
    }
  };

  /* package */ Tcs34725(I2cDevice device, int address) {
    super(device, address);
    handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  public Tcs34725 create(int address) {
    int fullAddress = BASE_ADDRESS + address;
    I2cDevice device = getDevice(getBus(), fullAddress);
    return new Tcs34725(device, fullAddress);
  }

  public void close() {
    super.close();
    if (handlerThread != null) {
      handlerThread.quitSafely();
    }
  }

  private void initGpio() {
    if (gpio == null) return;

    try {
      gpio.setDirection(Gpio.DIRECTION_IN);
      gpio.setActiveType(Gpio.ACTIVE_HIGH);
      gpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
      gpio.registerGpioCallback(gpioCallback);
    } catch (IOException e) {
      //e.printStackTrace();
    }
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setGain(int gain) {
    if (gain > 0 && gain < 4) {
      try {
        device.writeRegByte(CONTROL, (byte) gain);
      } catch (IOException e) {
        //
      }
    }
  }

  public void setIntegrationTime(int time) {

  }

  public void setInterruptThresholds(int lower, int upper) {
    byte[] buffer = new byte[]{(byte) (lower & 0xFF), (byte) (lower >> 4), (byte) (upper & 0xFF), (byte) (upper >> 4)};
    try {
      device.readRegBuffer(AILTL, buffer, buffer.length);
    } catch (IOException e) {
      //
    }
  }

  public void enableInterrupt(boolean enable) {
    try {
      int enableRegister = device.readRegByte(ENABLE);
      byte en = (byte) (enable ? (enableRegister | AIEN) : (enableRegister & ~AIEN));
      device.writeRegByte(ENABLE, en);
    } catch (IOException e) {
      //
    }
  }

  @Override
  public void run() {
    // read colour
  }

  public interface Listener {
    void onColour(int red, int green, int blue, int clear);
  }


}
