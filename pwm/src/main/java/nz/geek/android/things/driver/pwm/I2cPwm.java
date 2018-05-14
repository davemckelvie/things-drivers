/*
 * Copyright 2018 Dave McKelvie <www.android.geek.nz>.
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
package nz.geek.android.things.driver.pwm;

import com.google.android.things.pio.Pwm;

import java.io.IOException;

import nz.geek.android.things.driver.pca9685.Pca9685;

;

/**
 * A wrapper around the pca9685 that implements the {@code Pwm} interface. This is
 * to hide the fact that the relationship between a pca9685 and a PWM pin is a
 * 'one to many' and not the 'one to one' that you might expect between a PWM pin
 * and its controlling hardware.
 */
public class I2cPwm implements Pwm {
  private final String name;
  private final Pca9685 pca9685;
  private final int pin;
  private double dutyCycle;
  private double frequency;

  /**
   * This constructor expects the given name to be in the format
   * PWM_x_PIN_y where x is an arbitrary number specifying a particular
   * pca9685 and y is the pin number, i.e., 0-15
   * @param name e.g., PWM_0_PIN_3
   * @param pca9685 reference to the PWM controller IC
   */
  public I2cPwm(String name, Pca9685 pca9685) {
    this.name = name;
    this.pca9685 = pca9685;
    String[] parts = name.split("_");
    pin = Integer.decode(parts[parts.length - 1]);
  }

  @Override
  public void close() throws IOException {
    pca9685.close();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setPwmDutyCycle(double dutyCycle) throws IOException {
    pca9685.setPwmDutyCycle(dutyCycle, pin);
    this.dutyCycle = dutyCycle;
  }

  @Override
  public void setPwmFrequencyHz(double frequency) throws IOException {
    pca9685.setPwmFrequencyHz(frequency);
    this.frequency = frequency;
  }

  @Override
  public void setEnabled(boolean enabled) throws IOException {
    if (enabled) {
      pca9685.setPwmDutyCycle(dutyCycle, pin);
    } else {
      pca9685.setPin(false, pin);
    }
  }
}
