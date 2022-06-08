package catpoint.data;

import service.ImageServiceInterface;

import java.util.List;
import java.util.Set;

/**
 * Interface showing the methods our security repository will need to support
 */
public interface SecurityRepository {
    void addSensor(Sensor sensor);
    void removeSensor(Sensor sensor);
    void updateSensor(Sensor sensor);
    void setAlarmStatus(AlarmStatus alarmStatus);
    void setArmingStatus(ArmingStatus armingStatus);
    Set<Sensor> getSensors();
    AlarmStatus getAlarmStatus();
    ArmingStatus getArmingStatus();
    AlarmStatus pendingAlarmStatus(Sensor sensor, ArmingStatus armingStatus);
    AlarmStatus alarmStatus(ArmingStatus armingStatus, Sensor sensor, AlarmStatus alarmStatus);
    AlarmStatus noAlarmStatus(AlarmStatus alarmStatus, Set<Sensor> sensors);
    AlarmStatus sensorAlreadyActivated(Sensor sensor, boolean wishToActivate, AlarmStatus alarmStatus);
    AlarmStatus catDetectedAlarmStatus(Boolean isThereACat);
    AlarmStatus noCatDetected(Boolean isThereACat, Set<Sensor> sensors);
    AlarmStatus noAlarm(ArmingStatus armingStatus);
    Set<Sensor> resetSensors(ArmingStatus armingStatus, Set<Sensor> sensors);


}
