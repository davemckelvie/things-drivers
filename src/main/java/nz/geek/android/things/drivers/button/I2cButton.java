package nz.geek.android.things.drivers.button;

import android.util.ArrayMap;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.userdriver.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;
import java.util.Map;

import nz.geek.android.things.drivers.i2c.Pcf8574;

import static nz.geek.android.things.drivers.i2c.Pcf8574.BV;

public class I2cButton {
  private static final String TAG = I2cButton.class.getSimpleName();
  private static final int DRIVER_VERSION = 1;
  private static final int MAX_BUTTONS = 8; // 8 pin IO port

  private final Pcf8574 pcf8574;
  private final Gpio gpio;
  private final Map<Integer, Integer> buttonMap;
  private int buttonMask = 0;
  private int buttonValue = 0xFF;
  private InputDriver inputDriver;

  private I2cButton(int address, boolean isPcf8574, Gpio gpio, Map<Integer, Integer> buttonMap) {
    this.gpio = gpio;
    this.buttonMap = buttonMap;
    pcf8574 = Pcf8574.create(address, isPcf8574);
    initGpio();
    initButtonMask();
    initPort();
    initInputDriver();
  }

  /**
   * Initialise PCF8574 for input in accordance with datasheet
   */
  private void initPort() {
    pcf8574.writeByte(buttonMask, 0xFF);
  }

  /**
   * Initialise GPIO if set by builder method {@link I2cButtonBuilder#withInterrupt(Gpio)}. This
   * should be connected to PCF8574 INT pin.
   */
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

  /**
   * create a button mask for the pins set by the builder method {@link I2cButtonBuilder#addButton(int, int)}
   */
  private void initButtonMask() {
    for(int pin : buttonMap.keySet()) {
      buttonMask |= BV(pin);
    }
    buttonMask = ~buttonMask;
  }

  /**
   * convert Integer array to int array of keycodes for input driver builder method
   * @param input Integer array to convert
   * @return int array
   */
  private int[] toIntArray(Integer[] input) {
    int[] output = new int[input.length];
    for(int i = 0; i < input.length; i++) {
      output[i] = input[i];
    }
    return output;
  }

  /**
   * Initialise and register an input driver.
   */
  private void initInputDriver() {

    inputDriver = InputDriver.builder(InputDevice.SOURCE_CLASS_BUTTON)
            .setName(TAG)
            .setVersion(DRIVER_VERSION)
            .setKeys(toIntArray(buttonMap.values().toArray(new Integer[buttonMap.size()])))
            .build();

    UserDriverManager manager = UserDriverManager.getManager();
    manager.registerInputDriver(inputDriver);
  }

  /**
   * send a key event via input driver
   * @param pressed true when key pressed
   * @param keyCode the keycode of the pressed key
   */
  private void triggerEvent(boolean pressed, int keyCode) {
    int action = pressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
    KeyEvent[] events = new KeyEvent[] {new KeyEvent(action, keyCode)};
    inputDriver.emit(events);
  }

  /**
   * Read the current button state, send key event on state change
   */
  private void readButtons() {
    int buttons = pcf8574.readByte();

    if (buttons != buttonValue) {
      // What button has changes state?
      // "Output is 1 when both inputs are different." - Dr. Lee (XOR)
      int changedButtons = buttons ^ buttonValue;

      // TODO: seems like there should be a smarter way of doing this
      for (int key : buttonMap.keySet()) {
        int pinValue = BV(key);
        if ((pinValue & changedButtons) == pinValue) {
          triggerEvent(!((pinValue & buttons) == pinValue), buttonMap.get(key));
        }
      }

      buttonValue = buttons;
    }
  }

  private final GpioCallback gpioCallback = new GpioCallback() {
    @Override
    public boolean onGpioEdge(Gpio gpio) {
      readButtons();
      return true;
    }

    @Override
    public void onGpioError(Gpio gpio, int error) {
      Log.d(TAG, "onGpioError: ");
    }
  };

  public void close() {
    UserDriverManager manager = UserDriverManager.getManager();
    manager.unregisterInputDriver(inputDriver);
    if (gpio != null) {
      try {
        gpio.close();
      } catch (IOException e) {
        //
      }
    }
    if (pcf8574 != null) {
      pcf8574.close();
    }
  }

  public static I2cButtonBuilder builder() {
    return new I2cButtonBuilder();
  }

  public static final class I2cButtonBuilder {
    private int address;
    private boolean isPcf8574 = false;
    private Gpio gpio;
    private Map<Integer, Integer> buttonMap;

    private I2cButtonBuilder() {

    }

    /**
     * Specify the I2C address of the PCF8574. This is the value of A0-A2 pins, not
     * the base address of the PCF8574 which is inferred with {@link #isPcf8574(boolean)}
     * @param address value of A0-A2 [1:7]
     * @return
     */
    public final I2cButtonBuilder address(int address) {
      this.address = address;
      return this;
    }

    /**
     * Specify that the LCD is connected with a PCF8574. This uses a different
     * base address to the more typical PCF8574A.
     * @param isPcf8574 true when PCF8574 should be used
     * @return
     */
    public final I2cButtonBuilder isPcf8574(boolean isPcf8574) {
      this.isPcf8574 = isPcf8574;
      return this;
    }

    public final I2cButtonBuilder withInterrupt(Gpio gpio) {
      this.gpio = gpio;
      return this;
    }

    /**
     * Add a button with given pin and key code
     * @param pin the pin number on the PCF8574 of this button [0:7]
     * @param keyCode the key code of this button
     * @return the builder
     */
    public final I2cButtonBuilder addButton(int pin, int keyCode) {
      if (pin >= 0 && pin <= 7) {
        if (buttonMap == null) {
          buttonMap = new ArrayMap<>(MAX_BUTTONS);
        }
        buttonMap.put(pin, keyCode);
      }
      return this;
    }

    public I2cButton build() {
      return new I2cButton(address, isPcf8574, gpio, buttonMap);
    }

  }
}
