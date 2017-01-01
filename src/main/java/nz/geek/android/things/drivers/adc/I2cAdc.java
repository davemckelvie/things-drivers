package nz.geek.android.things.drivers.adc;

import android.os.Handler;
import android.os.HandlerThread;

import nz.geek.android.things.drivers.i2c.Pcf8591;

import static nz.geek.android.things.drivers.i2c.Pcf8591.ANALOG_OUTPUT_ENABLE;
import static nz.geek.android.things.drivers.i2c.Pcf8591.MODE_FOUR_SINGLE_ENDED;
import static nz.geek.android.things.drivers.i2c.Pcf8591.MODE_THREE_DIFFERENTIAL;
import static nz.geek.android.things.drivers.i2c.Pcf8591.MODE_TWO_DIFFERENTIAL;
import static nz.geek.android.things.drivers.i2c.Pcf8591.MODE_TWO_SINGLE_ONE_DIFFERENTIAL;

public class I2cAdc implements Adc {

  /**
   * read ADC every 50 ms
   */
  private static final int DEFAULT_RATE = 50;

  private static final int NUM_CHANNELS = 4;
  private static final int CHANNEL_MAX = 3;
  private static final int CHANNEL_MIN = 0;

  private int[] values = new int[NUM_CHANNELS];

  private final HandlerThread handlerThread;
  private final Handler handler;
  private final AdcReaderRunnable adcReaderRunnable = new AdcReaderRunnable();
  private final Pcf8591 pcf8591;
  private final int conversionRate;

  private I2cAdc(int address, int mode, int conversionRate) {
    this.conversionRate = conversionRate;
    handlerThread = new HandlerThread(Pcf8591.class.getSimpleName());
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    pcf8591 = Pcf8591.create(address);
    pcf8591.configure(ANALOG_OUTPUT_ENABLE | mode);
  }

  @Override
  public int readChannel(int channel) {
    if (channel < CHANNEL_MIN || channel > CHANNEL_MAX) return -1;
    return values[channel];
  }

  @Override
  public void startConversions() {
    handler.post(adcReaderRunnable);
  }

  @Override
  public void stopConversions() {
    handler.removeCallbacks(adcReaderRunnable);
  }

  @Override
  public void close() {
    stopConversions();
    handlerThread.quitSafely();
    pcf8591.close();
  }

  public static I2cAdcBuilder builder() {
    return new I2cAdcBuilder();
  }

  public static class I2cAdcBuilder {

    private int address;
    private int mode;
    private int rate = DEFAULT_RATE;

    public I2cAdcBuilder address(int address) {
      this.address = address;
      return this;
    }

    public I2cAdcBuilder fourSingleEnded() {
      mode = MODE_FOUR_SINGLE_ENDED;
      return this;
    }

    public I2cAdcBuilder threeDifferential() {
      mode = MODE_THREE_DIFFERENTIAL;
      return this;
    }

    public I2cAdcBuilder twoSingleOneDifferential() {
      mode = MODE_TWO_SINGLE_ONE_DIFFERENTIAL;
      return this;
    }

    public I2cAdcBuilder twoDifferential() {
      mode = MODE_TWO_DIFFERENTIAL;
      return this;
    }

    public I2cAdcBuilder withConversionRate(int rate) {
      this.rate = rate;
      return this;
    }

    public I2cAdc build() {
      return new I2cAdc(address, mode, rate);
    }
  }

  private class AdcReaderRunnable implements Runnable {
    private int currentChannel = 0;

    @Override
    public void run() {
      int[] rawValues = pcf8591.readAllChannels();
      for (int i = 0; i < CHANNEL_MAX; i++) {
        values[i] = (values[i] + rawValues[i]) / 2;
      }
//      values[currentChannel] = (values[currentChannel] + pcf8591.readChannel(currentChannel)) / 2;
//      if (++currentChannel > CHANNEL_MAX) {
//        currentChannel = CHANNEL_MIN;
//      }
      handler.postDelayed(this, conversionRate);
    }
  }
}
