package nz.geek.android.things.drivers.pwm;

import android.util.FloatProperty;

import java.io.IOException;

public class I2cPwmProperty extends FloatProperty<Servo> {

  private Float value;

  public I2cPwmProperty(String name) {
    super(name);
  }

  @Override
  public void setValue(Servo object, float value) {
    this.value = value;
    try {
      object.setAngle(value);
    } catch (IOException e) {
      // de nada
    }
  }


  @Override
  public Float get(Servo object) {
    return value;
  }
}
