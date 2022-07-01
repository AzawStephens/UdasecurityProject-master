package catpoint.application;

import catpoint.data.AlarmStatus;
import catpoint.data.ArmingStatus;

/**
 * Identifies a component that should be notified whenever the system status changes
 */
public interface StatusListener {
    void notify(AlarmStatus status);
    void catDetected(boolean catDetected);
    void sensorStatusChanged();
   //void notify(ArmingStatus status);
}
