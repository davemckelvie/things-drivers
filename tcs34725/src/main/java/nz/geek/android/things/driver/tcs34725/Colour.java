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
package nz.geek.android.things.driver.tcs34725;

public class Colour {
  public final int red;
  public final int green;
  public final int blue;
  public final int clear;

  public Colour(int red, int green, int blue, int clear) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.clear = clear;
  }

  /**
   * Create a {@link Colour} from the given byte array
   * @param data [CL, CH, RL, RH, GL, GR, BL, BH]
   * @return new {@link Colour}
   */
  public static Colour fromByteArray(byte[] data) {
    int clear = ((data[0] & 0xFF) | (data[1] << 8)) & 0xFFFF;
    int red   = ((data[2] & 0xFF) | (data[3] << 8)) & 0xFFFF;
    int green = ((data[4] & 0xFF) | (data[5] << 8)) & 0xFFFF;
    int blue  = ((data[6] & 0xFF) | (data[7] << 8)) & 0xFFFF;
    return new Colour(red, green, blue, clear);
  }

  /**
   * Calculate lux (Illuminance) for the given RGB values
   * @param red sensor value
   * @param green sensor value
   * @param blue sensor value
   * @return lux
   */
  public static int toLux(int red, int green, int blue) {
    return (int) ((-0.32466f * (float) red) + (1.57837f * (float) green) + (-0.73191f * (float) blue));
  }

  public int toLux() {
    return toLux(red, green, blue);
  }

  /**
   * Calculate the Correlated Colour Temperature (CCT) from the given
   * RGB values. Formula taken from http://ams.com/eng/content/view/download/145158
   * (TAOS Design Note 25 DN25)
   * @param red value from sensor
   * @param green value from sensor
   * @param blue value from sensor
   * @return CCT
   */
  public static int toColourTemperature(int red, int green, int blue) {
    float X, Y, Z, xc, yc, n;

    X = (-0.14282f * red) + (1.54924f * green) + (-0.95641f * blue);
    Y = (-0.32466f * red) + (1.57837f * green) + (-0.73191f * blue);
    Z = (-0.68202f * red) + (0.77073f * green) + ( 0.56332f * blue);

    xc = (X) / (X + Y + Z);
    yc = (Y) / (X + Y + Z);

    n = (xc - 0.3320F) / (0.1858F - yc);

    return (int) ((449.0F * Math.pow(n, 3)) + (3525.0F * Math.pow(n, 2)) + (6823.3F * n) + 5520.33F);
  }

  public int toColourTemperature() {
    return toColourTemperature(red, green, blue);
  }
}
