/*
 * Copyright 2017 Dave McKelvie
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
package nz.geek.android.things.drivers.colour;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import nz.geek.android.things.drivers.i2c.Tcs34725;

/**
 * Wrapper class for the TCS34725 colour sensor
 */
public class ColourSensor {

  private Tcs34725 tcs34725;
  private Gpio ledGpio;
  private String ledGpioName;
  private Gpio interruptGpio;
  private String interruptGpioName;

  private GpioCallback gpioCallback = new GpioCallback() {
    @Override
    public boolean onGpioEdge(Gpio gpio) {
      return super.onGpioEdge(gpio);
    }
  };

  /**
   * private constructor, use {@link ColourSensorBuilder} to create.
   * @param bus the I2C bus that the sensor is connected to.
   * @param ledGpio the {@link Gpio} that the sensor LED is connected to. (optional)
   * @param ledGpioName the name of the {@link Gpio}, a String returned by {@link PeripheralManagerService#getGpioList()} (optional)
   * @param interruptGpio the {@link Gpio} to use as and interrupt from the sensor. (optional)
   * @param interruptGpioName the name of the {@link Gpio}, a String returned by {@link PeripheralManagerService#getGpioList()} (optional)
   * @param listener a {@link ColourSensor.Listener} interested in sensor readings (optional)
   */
  private ColourSensor(String bus, Gpio ledGpio, String ledGpioName, Gpio interruptGpio, String interruptGpioName,
                       ColourSensor.Listener listener) {

    this.ledGpio = ledGpio;
    this.ledGpioName = ledGpioName;
    this.interruptGpio = interruptGpio;
    this.interruptGpioName = interruptGpioName;

    initLedGpio();
    initInterruptGpio();

    if (bus != null) {
      tcs34725 = Tcs34725.create(bus);
    } else {
      tcs34725 = Tcs34725.create();
    }
    tcs34725.setListener(listener);
  }

  /**
   * A listener to be notified of sensor readings
   */
  public interface Listener {
    void onColourUpdated(int clear, int red, int green, int blue);
  }

  private void initLedGpio() {
    if (ledGpio == null && ledGpioName == null) return;
    if (ledGpio == null) {
      PeripheralManagerService manager = new PeripheralManagerService();
      try {
        ledGpio  = manager.openGpio(ledGpioName);
      } catch (IOException e) {
        return;
      }
    }
    try {
      ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
      ledGpio.setActiveType(Gpio.ACTIVE_HIGH);
      // ledGpio.setValue(false);
    } catch (IOException e) {
      //
    }
  }

  private void initInterruptGpio() {
    if (interruptGpio == null && interruptGpioName == null) return;
    if (interruptGpio == null) {
      PeripheralManagerService manager = new PeripheralManagerService();
      try {
        interruptGpio = manager.openGpio(interruptGpioName);
      } catch (IOException e) {
        return;
      }
    }
    try {
      interruptGpio.setDirection(Gpio.DIRECTION_IN);
      interruptGpio.setActiveType(Gpio.ACTIVE_HIGH);
      interruptGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
      interruptGpio.registerGpioCallback(gpioCallback);
    } catch (IOException e) {
      //
    }
  }

  public void registerListener(ColourSensor.Listener listener) {
    if (tcs34725 == null) return;
    tcs34725.setListener(listener);
  }

  public void unregisterListener(ColourSensor.Listener listener) {
    if (tcs34725 == null) return;
    tcs34725.setListener(null);
  }

  public void enableLed(boolean enable) {
    if (ledGpio != null) {
      try {
        ledGpio.setValue(enable);
      } catch (IOException e) {
        //
      }
    }
  }

  public void enable(boolean enable) {
    if (tcs34725 != null) {
      tcs34725.enable(enable);
      enableLed(enable);
    }
  }

  public void close() {
    if (ledGpio != null) {
      try {
        ledGpio.close();
      } catch (IOException e) {
        //
      }
    }
    if (interruptGpio != null) {
      try {
        interruptGpio.unregisterGpioCallback(gpioCallback);
        interruptGpio.close();
      } catch (IOException e) {
        //
      }
    }
    if (tcs34725 != null) {
      tcs34725.close();
    }
  }

  public static ColourSensorBuilder builder() {
    return new ColourSensorBuilder();
  }

  public static final class ColourSensorBuilder {
    private String bus = null;
    private Gpio ledGpio = null;
    private String ledGpioName = null;
    private Gpio interruptGpio;
    private String interruptGpioName;
    private ColourSensor.Listener listener = null;

    /**
     * Specify the I2C bus that the colour sensor is connected to.
     * If not specified the first bus returned by {@link PeripheralManagerService#getI2cBusList()} is used.
     * @param bus the name of a I2C bus returned by {@link PeripheralManagerService#getI2cBusList()}
     * @return the builder
     */
    public ColourSensorBuilder withBus(String bus) {
      this.bus = bus;
      return this;
    }

    /**
     * Specify the {@link Gpio} connected to the module LED. This will be used to control a light source for
     * the sensor.
     * @param gpio the {@link Gpio} returned by {@link PeripheralManagerService#openGpio(String)}
     * @return the builder
     */
    public ColourSensorBuilder withLedGpio(Gpio gpio) {
      this.ledGpio = gpio;
      return this;
    }

    /**
     * Specify the {@link Gpio} connected to the module LED by name. Use this method instead of
     * {@link #withLedGpio(Gpio)} to use the name of the {@link Gpio}, one of the names returned
     * by {@link PeripheralManagerService#getGpioList()}
     * @param gpioName the name of the {@link Gpio} to use
     * @return the builder
     */
    public ColourSensorBuilder withLedGpio(String gpioName) {
      this.ledGpioName = gpioName;
      return this;
    }

    /**
     * Specify the {@link Gpio} connected to the colour sensor interrupt.
     * @param gpio the {@link Gpio} returned by {@link PeripheralManagerService#openGpio(String)}
     * @return the builder
     */
    public ColourSensorBuilder withInterruptGpio(Gpio gpio) {
      this.interruptGpio = gpio;
      return this;
    }

    /**
     * Specify the {@link Gpio} connected to the colour sensor interrupt by name. Use this method instead of
     * {@link #withInterruptGpio(Gpio)} to use the name of the {@link Gpio}, one of the names returned
     * by {@link PeripheralManagerService#getGpioList()}
     * @param gpioName the name of the {@link Gpio} to use
     * @return the builder
     */
    public ColourSensorBuilder withInterruptGpio(String gpioName) {
      this.interruptGpioName = gpioName;
      return this;
    }

    /**
     * Specify the listener to be notified of colour updates.
     * @param listener
     * @return the builder
     */
    public ColourSensorBuilder withListener(ColourSensor.Listener listener) {
      this.listener = listener;
      return this;
    }

    public ColourSensor build() {
      return new ColourSensor(bus, ledGpio, ledGpioName, interruptGpio, interruptGpioName, listener);
    }
  }
}
