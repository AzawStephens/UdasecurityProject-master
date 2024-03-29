package catpoint.service;
import catpoint.application.StatusListener;
import catpoint.data.*;
import service.ImageServiceInterface;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;


/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {
    private ImageServiceInterface imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private boolean catStat = false;
    ArmingStatus current;



    public SecurityService(SecurityRepository securityRepository, ImageServiceInterface imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;

    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */


    public  void setArmingStatus(ArmingStatus armingStatus) {

        if(armingStatus == ArmingStatus.DISARMED)
        {setAlarmStatus(AlarmStatus.NO_ALARM);}
        if(saveArmingStatus() == ArmingStatus.DISARMED && securityRepository.getCatStatus()) {

            setAlarmStatus(AlarmStatus.ALARM);
        }
        else if(armingStatus == ArmingStatus.ARMED_HOME || armingStatus == ArmingStatus.ARMED_AWAY) {

            resetSensors(securityRepository.getSensors());
//            for(Sensor s : securityRepository.getSensors())
//            {s.setActive(false);}
        }
        statusListeners.forEach(StatusListener::sensorStatusChanged);
        securityRepository.setArmingStatus(armingStatus);}
    public ArmingStatus saveArmingStatus()
    {
        current = getArmingStatus();
        return current;
    }

    public Set<Sensor> resetSensors(Set<Sensor> sensors)
    {
        for(Sensor s : sensors)
        {
            s.setActive(false);
        }
        return sensors;
    }
    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */



    void catDetected(Boolean cat) {

        statusListeners.forEach(sl -> sl.catDetected(cat));
        securityRepository.setCatStatus(cat);
        catStat = securityRepository.getCatStatus();

        if (catStat && getArmingStatus() == ArmingStatus.ARMED_HOME || catStat && getArmingStatus() == ArmingStatus.ARMED_AWAY) {
            setAlarmStatus(AlarmStatus.ALARM);

        }
        else if (!catStat && getArmingStatus() == ArmingStatus.ARMED_HOME  || !catStat && getArmingStatus() == ArmingStatus.ARMED_AWAY )
        {
            for(Sensor sensor : securityRepository.getSensors())
            {
                if (sensor.getActive())
                {
                    setAlarmStatus(AlarmStatus.ALARM);
                    securityRepository.updateSensor(sensor);
                    break;
                }else
                {
                    setAlarmStatus(AlarmStatus.NO_ALARM);
                }
            }
        }
        else {setAlarmStatus(AlarmStatus.NO_ALARM);}}

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }
//    public void removeStatusListener(StatusListener statusListener) {statusListeners.remove(statusListener);}
    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));}
    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    public void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            default -> setAlarmStatus(AlarmStatus.NO_ALARM);}
    }
    public AlarmStatus changeToPending(Sensor sensorStatus, ArmingStatus armingStatus) //Works with test 1
    {
        AlarmStatus alarmStatus = AlarmStatus.NO_ALARM;
        if(sensorStatus.getActive() && armingStatus.equals(ArmingStatus.ARMED_HOME) ) {
            alarmStatus = AlarmStatus.PENDING_ALARM;
        } else if (sensorStatus.getActive() && armingStatus.equals(ArmingStatus.ARMED_AWAY)) {
            alarmStatus =  AlarmStatus.PENDING_ALARM;}
        else{return alarmStatus;}
        return alarmStatus;}
    public AlarmStatus changeToAlarm(ArmingStatus armingStatus, Sensor sensor, AlarmStatus alarmStatus) //Works with test 2
    {
        switch (armingStatus)
        {
            case ARMED_AWAY, ARMED_HOME -> {
                if(sensor.getActive() && alarmStatus.equals(AlarmStatus.PENDING_ALARM)) {
                    return AlarmStatus.ALARM;}break;}
            case DISARMED -> {return AlarmStatus.NO_ALARM;}
        }
        return AlarmStatus.NO_ALARM;
    }
    public void noAlarmSet(AlarmStatus alarmStatus, Set<Sensor> sensors) //Works with test 3
    {
        boolean activeSensor = false;
        if(alarmStatus == AlarmStatus.PENDING_ALARM)
        {
            for(Sensor sensor: sensors)
            {
                if(sensor.getActive()) //if a sensor is active
                {
                    activeSensor = true;
                    break;
                }else{activeSensor = false;}

            }
        }if(activeSensor == false && alarmStatus == AlarmStatus.PENDING_ALARM)
        {
                securityRepository.setAlarmStatus(AlarmStatus.NO_ALARM);
        }else{securityRepository.setAlarmStatus(alarmStatus);}


//        if(alarmStatus.equals(AlarmStatus.PENDING_ALARM))
//        {return AlarmStatus.NO_ALARM;}
//        return AlarmStatus.PENDING_ALARM;
    }
    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    public void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }
    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) { //Works with test 4 GUI PORTION
        if(securityRepository.getAlarmStatus()!=AlarmStatus.ALARM) {
            if (!sensor.getActive() && active) {
                handleSensorActivated();
            } else if (sensor.getActive() && !active) {
                handleSensorDeactivated();
            }}
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    public AlarmStatus sensorAlreadyActivated(Sensor sensor, boolean wishToActivate, AlarmStatus alarmStatus) //Works with test 5
    {
        boolean alreadyActive = sensor.getActive();
        AlarmStatus thisAlarmStatus = AlarmStatus.NO_ALARM;
        if(alreadyActive && wishToActivate && alarmStatus.equals(AlarmStatus.PENDING_ALARM))
        {
            thisAlarmStatus = AlarmStatus.ALARM;
            securityRepository.setAlarmStatus(thisAlarmStatus);
        }else if(!alreadyActive && !wishToActivate)
        {
            thisAlarmStatus = alarmStatus;
            securityRepository.setAlarmStatus(thisAlarmStatus);
        }
        return thisAlarmStatus;
    }
    public AlarmStatus noCatNoAlarmSet(boolean isThereACat, Set<Sensor> sensors) //Works with test 8
    {
        if(!isThereACat) {
            for(Sensor sensor: sensors) {
                if(sensor.getActive()) {
                    return  AlarmStatus.PENDING_ALARM;
                }
            }
        }
        return AlarmStatus.NO_ALARM;
    }
    public AlarmStatus noAlarm(ArmingStatus armingStatus) //for test 9
    {
        if(armingStatus.equals(ArmingStatus.DISARMED))
        {
            return AlarmStatus.NO_ALARM;
        }else
        {
            return AlarmStatus.PENDING_ALARM;
        }
    }
    public Set<Sensor> resetTheSensors(ArmingStatus armingStatus, Set<Sensor> sensors) //for Test 10
    {
        if(armingStatus.equals(ArmingStatus.ARMED_AWAY) || armingStatus.equals(ArmingStatus.ARMED_HOME))
        {
            for(Sensor aSensor: sensors)
            {
                aSensor.setActive(false);
                securityRepository.updateSensor(aSensor);
            }
        }else if(armingStatus.equals(ArmingStatus.DISARMED))
        {
            return sensors;
        }
        return sensors;
    }
    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }
    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }
    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }
    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }
    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }
    public ArmingStatus getArmingStatus() {

        return securityRepository.getArmingStatus();

    }
}