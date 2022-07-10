package catpoint.service;
import catpoint.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.CheckReturnValue;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import service.ImageServiceInterface;
import org.junit.jupiter.params.ParameterizedTest;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository repository;
    private SecurityService securityService;
    private boolean active = true;
    private Sensor sensor = new Sensor();
    private AlarmStatus pendingAlarmStatus = AlarmStatus.PENDING_ALARM;
    @Mock
    private ImageServiceInterface imageServiceInterface;

    @Mock
    SecurityService securityServiceMock;

    @BeforeEach
    void init() {

        securityService = new SecurityService(repository, imageServiceInterface);
        securityServiceMock = new SecurityService();
    }


    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void pendingStatus_alarmIsArmed_SensorIsActivated_SystemReturnsPendingStatus(ArmingStatus armingStatus) //TEST 1
    {
        sensor.setActive(active);
        Assertions.assertEquals(AlarmStatus.PENDING_ALARM, securityService.changeToPending(sensor, armingStatus));

    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setStatusToAlarm_AlarmIsArmed_SensorIsActivated_SystemAlreadyPending_ReturnsAlamStatus(ArmingStatus armingStatus) //TEST 2
    {
        sensor.setActive(active);
        Assertions.assertEquals(AlarmStatus.ALARM, securityService.changeToAlarm(armingStatus, sensor, pendingAlarmStatus));
        Assertions.assertEquals(AlarmStatus.NO_ALARM, securityService.changeToAlarm(ArmingStatus.DISARMED, sensor, pendingAlarmStatus));
    }

    @Test
    void setStatusToNoAlarm_AlarmInPendingMode_NoSensorsAreActive_ReturnNoAlarmStatus() //TEST 3
    {
        Sensor sensor1 = new Sensor("Front Door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Back Door", SensorType.DOOR);
        boolean notActive = false;
        sensor1.setActive(notActive);
        sensor2.setActive(notActive);
        Set<Sensor> theSensors = new HashSet<>();
        theSensors.add(sensor1);
        theSensors.add(sensor2);
        Assertions.assertEquals(AlarmStatus.NO_ALARM, securityService.noAlarmSet(AlarmStatus.PENDING_ALARM, theSensors));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
        //TEST 4
    void alarmActive_ChangeInSensorMakesNoChanges_ReturnNoChangesToAlarmStatus(boolean status) {

        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);
        verify(repository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void sensorAlreadyActivated_SensorSetToActiveAndSystemPending_ReturnAlarmState() //TEST 5
    {
        Sensor sensor = new Sensor("Back Window", SensorType.WINDOW);
        sensor.setActive(true);
        boolean wishToActivate = true;
        Assertions.assertEquals(AlarmStatus.ALARM, securityService.sensorAlreadyActivated(sensor, wishToActivate, pendingAlarmStatus));
    }

    @Test
    void noChangesToAlarm_SensorIsNotActiveAlready_ReturnNoChange() //TEST 6
    {
        Sensor sensor = new Sensor("Back Window", SensorType.WINDOW);
        sensor.setActive(false);
        boolean wishToActivate = false;
        Assertions.assertEquals(pendingAlarmStatus, securityService.sensorAlreadyActivated(sensor, wishToActivate, pendingAlarmStatus));
    }

    @Test
    void catDetected_SystemInAtHomeStatus_ReturnsALARMIfCatIsFound() //TEST 7
    {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageServiceInterface.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);
        verify(repository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void noCatNoAlarm_NoSensorsActivated_ReturnNoAlarm() //Test 8
    {
        Sensor sensor1 = new Sensor("Back door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Front window", SensorType.WINDOW);
        boolean isActive = false;
        boolean catDetected = false;
        sensor1.setActive(isActive);
        sensor2.setActive(isActive);
        Set<Sensor> theSensors = new HashSet<>();
        theSensors.add(sensor1);
        theSensors.add(sensor2);
        Assertions.assertEquals(AlarmStatus.NO_ALARM, securityService.noCatNoAlarmSet(catDetected, theSensors));
    }

    @Test
    void noAlarm_systemDisarmed_ReturnNoAlarm() // TEST 9
    {
        ArmingStatus armingStatus = ArmingStatus.DISARMED;
        Assertions.assertEquals(AlarmStatus.NO_ALARM, securityService.noAlarm(armingStatus));  //invokes the call to noAlarm
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorReset_systemIsArmed_ReturnInactiveSensors(ArmingStatus armingStatus) //TEST 10
    {
        Sensor sensor1 = new Sensor("Back door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Front sensor", SensorType.MOTION);
        sensor1.setActive(true);
        sensor2.setActive(true);
        Set<Sensor> theSensors = new HashSet<>();
        theSensors.add(sensor1);
        theSensors.add(sensor2);
        Assertions.assertEquals(theSensors, securityService.resetTheSensors(armingStatus, theSensors));
        for (Sensor aSensor : theSensors) {
            verify(repository, atLeastOnce()).updateSensor(aSensor);
        }
    }

    @Test
    void catDetectedAgain_SystemInAtHomeStatus_ReturnsALARMIfCatIsFound() //TEST 11
    {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageServiceInterface.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

    }

    @Test
    void setArmingStatus_changeArmingStatus_returnChangedArmingStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(repository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void changeSensorAcivationStatus_runSensorDeactivated(boolean status) {
        Sensor aSensor = new Sensor("Front Door", SensorType.DOOR);
        aSensor.setActive(status);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(aSensor, false);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handleSensorActivatedTest(boolean status)
    {
        Sensor aSensor = new Sensor("Front Door", SensorType.DOOR);
        aSensor.setActive(status);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(aSensor, true);
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handleSensorDeactivatedTest(boolean status)
    {
        Sensor aSensor = new Sensor("Front Door", SensorType.DOOR);
        aSensor.setActive(status);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.handleSensorDeactivated();
    }

}
