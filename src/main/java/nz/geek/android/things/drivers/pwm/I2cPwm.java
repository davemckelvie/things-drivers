package nz.geek.android.things.drivers.pwm;

import com.google.android.things.pio.Pwm;

import java.io.IOException;

import nz.geek.android.things.drivers.i2c.Pcf9685;

/**
 * A wrapper around the pcf9685 that implements the {@code Pwm} interface. This is
 * to hide the fact that the relationship between a pcf9685 and a PWM pin is a
 * 'one to many' and not the 'one to one' that you might expect between a PWM pin
 * and its controlling hardware.
 */
public class I2cPwm implements Pwm {
  private final String name;
  private final Pcf9685 pcf9685;
  private final int pin;
  private double dutyCycle;
  private double frequency;

  /**
   * This constructor expects the given name to be in the format
   * PWM_x_PIN_y where x is an arbitrary number specifying a particular
   * pcf9685 and y is the pin number, i.e., 0-15
   * @param name e.g., PWM_0_PIN_3
   * @param pcf9685 reference to the PWM controller IC
   */
  public I2cPwm(String name, Pcf9685 pcf9685) {
    this.name = name;
    this.pcf9685 = pcf9685;
    String[] parts = name.split("_");
    pin = Integer.decode(parts[parts.length - 1]);
  }

  @Override
  public void close() throws IOException {
    pcf9685.close();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setPwmDutyCycle(double dutyCycle) throws IOException {
    pcf9685.setPwmDutyCycle(dutyCycle, pin);
    this.dutyCycle = dutyCycle;
  }

  @Override
  public void setPwmFrequencyHz(double frequency) throws IOException {
    pcf9685.setPwmFrequencyHz(frequency);
    this.frequency = frequency;
  }

  @Override
  public void setEnabled(boolean enabled) throws IOException {
    if (enabled) {
      pcf9685.setPwmDutyCycle(dutyCycle, pin);
    } else {
      pcf9685.setPin(false, pin);
    }
  }
}
