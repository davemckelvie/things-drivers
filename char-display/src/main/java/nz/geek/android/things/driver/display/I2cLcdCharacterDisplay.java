/*
 * Copyright 2016 Dave McKelvie <www.android.geek.nz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.geek.android.things.driver.display;

import com.google.android.things.pio.PeripheralManager;

import nz.geek.android.things.driver.pcf8574.Pcf8574;

import static nz.geek.android.things.driver.pcf8574.Pcf8574.BV;

public class I2cLcdCharacterDisplay extends AbstractLcdCharacterDisplay {

  private Pcf8574 pcf8574;

  /**
   * Read / Write pin bit value
   */
  private int rw = 0;

  /**
   * Register select pin bit value
   */
  private int rs = 0;

  /**
   * 40x4 displays have two enable pins, they're effectively two displays
   * this could be BV(ePin) or BV(e2Pin)
   */
  private int en = 0;

  /**
   * control mask will change when using 40x4 displays with two enable pins
   */
  private int controlMask;

  /**
   * data mask bit value
   */
  private final int dataMask;

  private final int address;
  private final boolean isPcf8574;
  private final String bus;

  /**
   * pin numbers [0:7]
   */
  private final int ePin;
  private final int e2Pin;
  private final int rsPin;
  private final int rwPin;
  private final int blPin;

  private I2cLcdCharacterDisplay(int width, int height, int address,
                                 int ePin, int e2Pin, int rsPin, int rwPin,
                                 int d4Pin, int d5Pin, int d6Pin, int d7Pin,
                                 boolean isPcf8574, boolean hasBl, int blPin,
                                 String bus) {
    super(width, height, hasBl);
    this.address = address;
    this.isPcf8574 = isPcf8574;
    this.ePin = ePin;
    this.e2Pin = e2Pin;
    this.rsPin = rsPin;
    this.rwPin = rwPin;
    this.blPin = blPin;
    this.bus = bus;

    // setup the masks and bit positions from pin numbers
    en = BV(ePin);
    rs = BV(rsPin);
    rw = BV(rwPin);

    // initialise data and control masks, data does not affect port when mask bit is 1
    dataMask = ~(BV(d4Pin) | BV(d5Pin) | BV(d6Pin) | BV(d7Pin));
    controlMask = ~(en | rs | rw);

    createPort();
  }

  /**
   * create the I2C IO port (PCF8574)
   */
  private void createPort() {
    if (bus != null) {
      pcf8574 = Pcf8574.create(address, bus, isPcf8574);
    } else {
      pcf8574 = Pcf8574.create(address, isPcf8574);
    }
  }

  @Override
  public void connect() {
    if (pcf8574 == null) createPort();
    pcf8574.writeByte(0x00, 0x00);

    init();
  }

  @Override
  public void disconnect() {
    if (pcf8574 != null) {
      pcf8574.close();
      pcf8574 = null;
    }
  }

  @Override
  public void enable(boolean enable) {
    // TODO: 13/09/17  
  }

  /**
   * write byte to the display
   * @param data
   */
  protected void write(int data) {
    pcf8574.writeByte(controlMask, rs);         // RS = rs, E = 0, R/W = 0
    pcf8574.writeByte(controlMask, rs | en);    // RS = rs, E = 1, R/W = 0
    pcf8574.writeByte(dataMask, data);          // D0-7 = data
    pcf8574.writeByte(controlMask, rs);         // RS = rs, E = 0, R/W = 0

    if (doubleWrite) {
      pcf8574.writeByte(controlMask, rs | en);  // RS = rs, E = 1, R/W = 0
      pcf8574.writeByte(dataMask, data << 4);
      pcf8574.writeByte(controlMask, rs);       // RS = rs, E = 0, R/W = 0
    }
  }

  protected void writeCommand(int command) {
    rs = 0;
    write(command);
    rs = BV(rsPin);
  }

  protected void switchDisplay(int display) {
    if (display == 2) {
      en = BV(e2Pin);
    } else {
      en = BV(ePin);
    }
    controlMask = ~(en | rs | rw);
  }

  @Override
  public void enableBackLight(boolean enable) {
    if (hasBackLight) {
      pcf8574.setPin(blPin, enable);
    }
  }

  public static Builder builder(int width, int height) {
    return new Builder(width, height);
  }

  /**
   * Builder used to create a {@link #I2cLcdCharacterDisplay(int, int, int, int, int, int, int, int, int, int, int, boolean, boolean, int, String)}
   */
  public static final class Builder {
    private int width;
    private int height;

    // port pin numbers [0:7]
    private int e1Pin;
    private int e2Pin;
    private int rsPin;
    private int rwPin;
    private int blPin;
    private int d4Pin, d5Pin, d6Pin, d7Pin;

    private int address;
    private boolean isPcf8574 = false; // i.e., not pcf8574A, default to no
    private boolean hasBackLight = false;
    private String bus = null;

    /*package*/ Builder(int width, int height) {
      this.width = width;
      this.height = height;
    }

    /**
     * Specify that the LCD is connected with a PCF8574. This uses a different
     * base address to the more typical PCF8574A.
     * @param isPcf8574 true when PCF8574 should be used
     * @return
     */
    public Builder isPcf8574(boolean isPcf8574) {
      this.isPcf8574 = isPcf8574;
      return this;

    }

    /**
     * Specify which pin of the PCF8574 is the LCD Enable pin
     * @param e1Pin enable pin [0:7]
     * @return
     */
    public Builder e(int e1Pin) {
      this.e1Pin = e1Pin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 is the second Enable pin. Large
     * LCD displays, more than 80 characters have 2 enable pins, one for
     * the first 'half' of the display, one for the second.
     * @param e2Pin Second anable pin [0:7]
     * @return
     */
    public Builder e2(int e2Pin) {
      this.e2Pin = e2Pin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 is the LCD Register Select pin.
     * @param rsPin rs pin [0:7]
     * @return
     */
    public Builder rs(int rsPin) {
      this.rsPin = rsPin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 is the Read/Write pin
     * @param rwPin rw pin [0:7]
     * @return
     */
    public Builder rw(int rwPin) {
      this.rwPin = rwPin;
      return this;
    }

    /**
     * Specify which pin of the PCF8574 controls the LCD backlight. The LCM1602
     * uses one of the PCF8574 pins to control the LCD backlight. Some modules use
     * another means of driving the LCD backlight and use this pin for the second
     * LCD enable pin on large LCDs. If your module uses the PCF8574 to control
     * the backlight specify it here. If it is not specified {@link #enableBackLight(boolean)} has no effect.
     * @param blPin backlight control pin [0:7]
     * @return
     */
    public Builder bl(int blPin) {
      this.blPin = blPin;
      hasBackLight = true;
      return this;
    }

    /**
     * specify which pins of the PCF8574 are connected to the LCD data lines D4-D7
     * @param d4Pin LCD D4 pin
     * @param d5Pin LCD D5 pin
     * @param d6Pin LCD D6 pin
     * @param d7Pin LCD D7 pin
     * @return
     */
    public Builder data(int d4Pin, int d5Pin, int d6Pin, int d7Pin) {
      this.d4Pin = d4Pin;
      this.d5Pin = d5Pin;
      this.d6Pin = d6Pin;
      this.d7Pin = d7Pin;
      return this;
    }

    /**
     * Specify the I2C address of the PCF8574. This is the value of A0-A2 pins, not
     * the base address of the PCF8574 which is inferred with {@link #isPcf8574(boolean)}
     * @param address value of A0-A2 [1:7]
     * @return
     */
    public Builder address(int address) {
      this.address = address;
      return this;
    }

    /**
     * Specify the name of the I2C bus that the LCD is connected to
     * @param bus the name of the bus returned from {@link PeripheralManager#getI2cBusList()}
     * @return the builder
     */
    public Builder withBus(String bus) {
      this.bus = bus;
      return this;
    }

    /**
     * Build the LCD
     * @return A new {@link #I2cLcdCharacterDisplay(int, int, int, int, int, int, int, int, int, int, int, boolean, boolean, int, String)} with your pin mapping.
     */
    public I2cLcdCharacterDisplay build() {
      return new I2cLcdCharacterDisplay(width, height, address, e1Pin, e2Pin, rsPin, rwPin,
              d4Pin, d5Pin, d6Pin, d7Pin, isPcf8574, hasBackLight, blPin, bus);
    }
  }
}
