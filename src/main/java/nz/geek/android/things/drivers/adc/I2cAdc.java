package nz.geek.android.things.drivers.adc;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import nz.geek.android.things.drivers.i2c.Pcf8591;

import static nz.geek.android.things.drivers.i2c.Pcf8591.ANALOG_OUTPUT_ENABLE;
import static nz.geek.android.things.drivers.i2c.Pcf8591.AUTO_INCREMENT;

public class I2cAdc implements Adc {
  private static final String TAG = "TingTing";

  private static final int CONVERSION_PERIOD = 100;
  private static final int NUM_CHANNELS = 4;
  private static final int CHANNEL_MAX = 3;
  private static final int CHANNEL_MIN = 0;

  private int[] values = new int[NUM_CHANNELS];

  private HandlerThread handlerThread;
  private Handler handler;
  private AdcReaderRunnable adcReaderRunnable = new AdcReaderRunnable();
  private final int address;
  private final Pcf8591 pcf8591;

  public I2cAdc(int address) {
    this.address = address;
    handlerThread = new HandlerThread(Pcf8591.class.getSimpleName());
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    pcf8591 = Pcf8591.create(address);
    pcf8591.configure(ANALOG_OUTPUT_ENABLE);
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

  private class AdcReaderRunnable implements Runnable {
    private int currentChannel = 0;

    @Override
    public void run() {
      values[currentChannel] = (values[currentChannel] + pcf8591.readChannel(currentChannel)) / 2;
      if (++currentChannel > CHANNEL_MAX) {
        currentChannel = CHANNEL_MIN;
      }
      handler.postDelayed(this, CONVERSION_PERIOD);
    }
  }
}
