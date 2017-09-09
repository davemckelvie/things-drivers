package nz.geek.android.things.drivers.led;

import java.nio.ByteBuffer;
import java.util.Arrays;

import nz.geek.android.things.drivers.display.CharacterDisplay;
import nz.geek.android.things.drivers.spi.LedMatrixController;

public class LedCharacterDisplay implements CharacterDisplay {

  private LedMatrixController device;
  private final int width;
  private final int height;

  // some ascii characters
  private static final byte STX = 5;
  private static final byte ETX = 6;
  private static final byte SPACE = 0x20;

  public LedCharacterDisplay(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public void connect() {
    if (device == null) {
      device = LedMatrixController.create();
    }
  }

  @Override
  public void disconnect() {
    if (device != null) {
      device.close();
      device = null;
    }
  }

  private byte[] toPacket(int line, String message) {
    ByteBuffer bb = ByteBuffer.allocate(3 + message.length());
    bb.put(STX);
    bb.put((byte)(line & 0xFF));
    bb.put(message.getBytes());
    bb.put(ETX);
    return bb.array();
  }

  private byte[] toPacket(byte[] message) {
    ByteBuffer bb = ByteBuffer.allocate(message.length + 2);
    bb.put(STX);
    bb.put(message);
    bb.put(ETX);
    return bb.array();
  }

  @Override
  public void print(int line, String message) {
    device.write(toPacket(line, message));
  }

  @Override
  public void clearLine(int line) {
    byte[] buffer = new byte[1 + width];
    Arrays.fill(buffer, SPACE);
    buffer[0] = (byte) (line & 0xFF);
    device.write(toPacket(buffer));
  }

  @Override
  public void clearDisplay() {
    for (int i = 0; i < height; i++) {
      clearLine(i + 1);
    }
  }

  @Override
  public void enableBackLight(boolean enable) {
    // no-op
  }

  @Override
  public boolean hasBackLight() {
    return false;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public void setCustomCharacter(int address, byte[] pattern) {
    // TODO: 10/09/17
  }

  @Override
  public void initBarGraph() {
    // TODO: 10/09/17
  }
}
