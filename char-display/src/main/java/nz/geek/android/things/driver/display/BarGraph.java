package nz.geek.android.things.driver.display;

import android.util.IntProperty;

public class BarGraph extends IntProperty<CharacterDisplay> {
  private static final char BAR_0 = 0x00;
  private static final char BAR_1 = 0x01;
  private static final char BAR_2 = 0x02;
  private static final char BAR_3 = 0x03;
  private static final char BAR_4 = 0x04;
  private static final char BAR_5 = 0x05;

  private int value;
  private boolean initialised;
  private final int line;

  public BarGraph(int line) {
    super("LCD Bargraph");
    this.line = line;
    initialised = false;
  }

  private void init(CharacterDisplay display) {
    initialised = true;
    display.setCustomCharacter(0x00, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
    display.setCustomCharacter(0x08, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x00});
    display.setCustomCharacter(0x10, new byte[]{0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x00});
    display.setCustomCharacter(0x18, new byte[]{0x1C, 0x1C, 0x1C, 0x1C, 0x1C, 0x1C, 0x1C, 0x00});
    display.setCustomCharacter(0x20, new byte[]{0x1E, 0x1E, 0x1E, 0x1E, 0x1E, 0x1E, 0x1E, 0x00});
    display.setCustomCharacter(0x28, new byte[]{0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x00});
  }

  private String createBarGraphString(int width, int value) {
    StringBuilder sb = new StringBuilder(width);
    int numFullCharacters = value / 5;
    int remainder = value % 5;
    int numEmptyCharacters = width - (numFullCharacters + (remainder == 0 ? 0 : 1));

    for (int i = 0; i < numFullCharacters; i++) {
      sb.append(BAR_5);
    }

    switch(remainder) {
      case 4:
        sb.append(BAR_4);
        break;
      case 3:
        sb.append(BAR_3);
        break;
      case 2:
        sb.append(BAR_2);
        break;
      case 1:
        sb.append(BAR_1);
        break;
    }

    for (int i = 0; i < numEmptyCharacters; i++) {
      sb.append(BAR_0);
    }

    return sb.toString();
  }

  private void updateBarGraph(CharacterDisplay display, int value) {
    this.value = value;
    if (!initialised) {
      init(display);
    }
    display.print(line, createBarGraphString(display.getWidth(), value));
  }

  @Override
  public void setValue(CharacterDisplay display, int i) {
    updateBarGraph(display, i);
  }

  @Override
  public Integer get(CharacterDisplay display) {
    return value;
  }
}
