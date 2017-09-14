package nz.geek.android.things.drivers.lcd;

import com.google.android.things.pio.Gpio;


public class LcdCharacterDisplay extends AbstractCharacterDisplay {

  private final Gpio e1Pin;
  private final Gpio e2Pin;
  private final Gpio rsPin;
  private final Gpio rwPin;
  private final Gpio blPin;
  private final Gpio d4Pin;
  private final Gpio d5Pin;
  private final Gpio d6Pin;
  private final Gpio d7Pin;

  private LcdCharacterDisplay(int width, int height, Gpio e1Pin, Gpio e2Pin, Gpio rsPin, Gpio rwPin,
                              Gpio blPin, Gpio d4Pin, Gpio d5Pin, Gpio d6Pin, Gpio d7Pin, boolean hasBackLight) {
    super(width, height, hasBackLight);
    this.e1Pin = e1Pin;
    this.e2Pin = e2Pin;
    this.rsPin = rsPin;
    this.rwPin = rwPin;
    this.blPin = blPin;
    this.d4Pin = d4Pin;
    this.d5Pin = d5Pin;
    this.d6Pin = d6Pin;
    this.d7Pin = d7Pin;
  }

  @Override
  public void connect() {

  }

  @Override
  public void disconnect() {

  }

  @Override
  public void enable(boolean enable) {

  }

  @Override
  public void print(int line, String message) {

  }

  @Override
  public void clearLine(int line) {

  }

  @Override
  public void clearDisplay() {

  }

  @Override
  public void enableBackLight(boolean enable) {

  }

  @Override
  public boolean hasBackLight() {
    return false;
  }

  @Override
  protected void write(int data) {

  }

  @Override
  protected void writeCommand(int command) {

  }

  @Override
  protected void switchDisplay(int display) {

  }

  @Override
  public void setCustomCharacter(int address, byte[] pattern) {

  }

  @Override
  public int setCustomCharacter(byte[] pattern) {
    return 0;
  }

  @Override
  public void initBarGraph() {

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


    public Builder(int width, int height) {
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
