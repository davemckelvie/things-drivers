package nz.geek.android.things.drivers.lcd;

import com.google.android.things.pio.Gpio;

import java.io.IOException;


public class LcdCharacterDisplay extends AbstractLcdCharacterDisplay {

  private Gpio ePin;

  private static final int E1 = 0;
  private static final int E2 = 1;
  private static final int RS = 2;
  private static final int RW = 3;
  private static final int D4 = 4;
  private static final int D5 = 5;
  private static final int D6 = 6;
  private static final int D7 = 7;
  private static final int BL = 8;
  private Gpio[] gpios = new Gpio[9];

  private LcdCharacterDisplay(int width, int height, Gpio e1Pin, Gpio e2Pin, Gpio rsPin, Gpio rwPin,
                              Gpio blPin, Gpio d4Pin, Gpio d5Pin, Gpio d6Pin, Gpio d7Pin, boolean hasBackLight) {
    super(width, height, hasBackLight);
    gpios[E1] = e1Pin;
    gpios[E2] = e2Pin;
    gpios[RS] = rsPin;
    gpios[RW] = rwPin;
    gpios[BL] = blPin;
    gpios[D4] = d4Pin;
    gpios[D5] = d5Pin;
    gpios[D6] = d6Pin;
    gpios[D7] = d7Pin;
    ePin = e1Pin;
  }

  @Override
  public void connect() {
    for (Gpio gpio : gpios) {
      if (gpio != null) {
        try {
          gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
          gpio.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e) {
          //e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void disconnect() {
    for (Gpio gpio : gpios) {
      if (gpio != null) {
        try {
          gpio.close();
        } catch (IOException e) {
          //
        }
      }
    }
  }

  @Override
  public void enable(boolean enable) {

  }

  @Override
  protected void switchDisplay(int display) {
    if (display == 2) {
      ePin = gpios[E2];
    } else {
      ePin = gpios[E1];
    }
  }

  @Override
  public void enableBackLight(boolean enable) {
    if (hasBackLight) {
      try {
        gpios[BL].setValue(enable);
      } catch (IOException e) {
        //
      }
    }
  }

  private void writeData(int data) {
    try {
      gpios[D7].setValue(((data & 0x80) == 0x80));
      gpios[D6].setValue(((data & 0x40) == 0x40));
      gpios[D5].setValue(((data & 0x20) == 0x20));
      gpios[D4].setValue(((data & 0x10) == 0x10));
    } catch (IOException e) {
      //
    }
  }
  @Override
  protected void write(int data) {
    try {
      // RS = rs, E = 0, R/W = 0
      ePin.setValue(false);
      gpios[RW].setValue(false);

      // RS = rs, E = 1, R/W = 0
      ePin.setValue(true);

      // D0-7 = data
      writeData(data);

      // RS = rs, E = 0, R/W = 0
      ePin.setValue(false);

      if (doubleWrite) {
        // RS = rs, E = 1, R/W = 0
        ePin.setValue(true);

        writeData(data << 4);

        // RS = rs, E = 0, R/W = 0
        ePin.setValue(false);
      }
    } catch (IOException e) {
      //
    }
  }

  @Override
  protected void writeCommand(int command) {
    try {
      gpios[RS].setValue(false);
      write(command);
      gpios[RS].setValue(true);
    } catch (IOException e) {
      //
    }

  }

  public static LcdCharacterDisplay.Builder builder(int width, int height) {
    return new Builder(width, height);
  }

  public static final class Builder{
    private final int width;
    private final int height;
    private boolean hasBackLight = false;

    private Gpio e1Pin;
    private Gpio e2Pin;
    private Gpio rsPin;
    private Gpio rwPin;
    private Gpio blPin;
    private Gpio d4Pin;
    private Gpio d5Pin;
    private Gpio d6Pin;
    private Gpio d7Pin;


    private Builder(int width, int height) {
      this.width = width;
      this.height = height;
    }

    public Builder e(Gpio e1Pin) {
      this.e1Pin = e1Pin;
      return this;
    }

    public Builder e2(Gpio e2Pin) {
      this.e2Pin = e2Pin;
      return this;
    }

    public Builder rs(Gpio rsPin) {
      this.rsPin = rsPin;
      return this;
    }

    public Builder rw(Gpio rwPin) {
      this.rwPin = rwPin;
      return this;
    }

    public Builder bl(Gpio blPin) {
      this.blPin = blPin;
      hasBackLight = true;
      return this;
    }

    public Builder data(Gpio d4Pin, Gpio d5Pin, Gpio d6Pin, Gpio d7Pin) {
      this.d4Pin = d4Pin;
      this.d5Pin = d5Pin;
      this.d6Pin = d6Pin;
      this.d7Pin = d7Pin;
      return this;
    }

    public LcdCharacterDisplay build() {
      return new LcdCharacterDisplay(width, height, e1Pin, e2Pin, rsPin, rwPin, blPin,
              d4Pin, d5Pin, d6Pin, d7Pin, hasBackLight);
    }
  }
}
