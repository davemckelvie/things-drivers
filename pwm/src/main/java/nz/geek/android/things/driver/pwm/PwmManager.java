package nz.geek.android.things.driver.pwm;

import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.geek.android.things.driver.pca9685.Pca9685;


public class PwmManager {

  private static final String TAG = "PwmManager";
  private static PwmManager INSTANCE;
  private List<Pca9685> pca9685List = new ArrayList<>();
  private Map<String, Pwm> pwmMap = new HashMap<>();
  private PwmManager(){}

  public static PwmManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PwmManager();
    }
    return INSTANCE;
  }

  public List<String> getPwmList() {
    PeripheralManager peripheralManager = PeripheralManager.getInstance();
    List<String> i2cList = peripheralManager.getI2cBusList();
    for (String bus : i2cList) {
      Pca9685 pca9685 = Pca9685.create(0, bus);
      if (pca9685 != null) {
        pca9685List.add(pca9685);
      }
    }
    createPwmMap();
    return new ArrayList<>(pwmMap.keySet());
  }

  /**
   * create a mapping between a PWM name and it
   */
  private void createPwmMap() {
    for (int i = 0; i < pca9685List.size(); i++) {
      for (int j = 0; j < 16; j++) {
        String name = "PWM_" + i + "_PIN_" + j;
        Log.d(TAG, "createPwmMap: " + name);
        pwmMap.put(name, new I2cPwm(name, pca9685List.get(i)));
      }
    }
  }

  public Pwm openPwm(String name) {
    if (pwmMap.containsKey(name)) {
      return pwmMap.get(name);
    }
    return null;
  }
}
