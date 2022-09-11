package catpoint.service;
import catpoint.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import service.ImageServiceInterface;
import org.junit.jupiter.params.ParameterizedTest;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository repository;

    @Mock
    private Sensor sensorMock;

    @Mock
    private SecurityService securityService;
    private boolean active = true;
    private Sensor sensor = new Sensor();
    private AlarmStatus pendingAlarmStatus = AlarmStatus.PENDING_ALARM;
    @Mock
    private ImageServiceInterface imageServiceInterface;

    @Mock
    private SecurityService securityServiceMock;

    @BeforeEach
    void init() {

        securityService = new SecurityService(repository, imageServiceInterface);
    }


    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void pendingStatus_alarmIsArmed_SensorIsActivated_SystemReturnsPendingStatus(ArmingStatus armingStatus) //TEST 1
    {
        sensor.setActive(active);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.changeToPending(sensor, armingStatus));

    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setStatusToAlarm_AlarmIsArmed_SensorIsActivated_SystemAlreadyPending_ReturnsAlamStatus(ArmingStatus armingStatus) //TEST 2
    {
        sensor.setActive(active);
        assertEquals(AlarmStatus.ALARM, securityService.changeToAlarm(armingStatus, sensor, pendingAlarmStatus));
        assertEquals(AlarmStatus.NO_ALARM, securityService.changeToAlarm(ArmingStatus.DISARMED, sensor, pendingAlarmStatus));
    }

    @Test
    void setStatusToNoAlarm_AlarmInPendingMode_NoSensorsAreActive_ReturnNoAlarmStatus() //TEST 3
    {
        Sensor sensor1 = new Sensor("Front Door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Back Door", SensorType.DOOR);
        sensor1.setActive(false);
        sensor2.setActive(false);
        Set<Sensor> theSensors = new HashSet<>();
        theSensors.add(sensor1);
        theSensors.add(sensor2);
        assertEquals(AlarmStatus.NO_ALARM, securityService.noAlarmSet(AlarmStatus.PENDING_ALARM, theSensors));

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
        assertEquals(AlarmStatus.ALARM, securityService.sensorAlreadyActivated(sensor, wishToActivate, pendingAlarmStatus));
    }

    @Test
    void noChangesToAlarm_SensorIsNotActiveAlready_ReturnNoChange() //TEST 6
    {
        Sensor sensor = new Sensor("Back Window", SensorType.WINDOW);
        sensor.setActive(false);
        boolean wishToActivate = false;
        assertEquals(pendingAlarmStatus, securityService.sensorAlreadyActivated(sensor, wishToActivate, pendingAlarmStatus));
    }

    @Test
    void catDetected_SystemInAtHomeStatus_ReturnsALARMIfCatIsFound() //TEST 7
    {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageServiceInterface.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);
        repository.setArmingStatus(ArmingStatus.ARMED_HOME);
        repository.setCatStatus(true);
        securityService.catDetected(repository.getCatStatus());

        verify(repository, atLeastOnce()).getArmingStatus();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void noCatNoAlarm_NoSensorsActivated_ReturnNoAlarm(boolean isActiveValue) //Test 8
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
        assertEquals(AlarmStatus.NO_ALARM, securityService.noCatNoAlarmSet(catDetected, theSensors));
        isActive = true;
        sensor1.setActive(isActive);
        sensor2.setActive(isActive);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.noCatNoAlarmSet(catDetected, theSensors));

    }

    @Test
    void noAlarm_systemDisarmed_ReturnNoAlarm() // TEST 9
    {
        ArmingStatus armingStatus = ArmingStatus.DISARMED;
        assertEquals(AlarmStatus.NO_ALARM, securityService.noAlarm(armingStatus));  //invokes the call to noAlarm
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.noAlarm(ArmingStatus.ARMED_HOME));
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
        assertEquals(theSensors, securityService.resetTheSensors(armingStatus, theSensors));
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

    /**@Test
    void setArmingStatus_changeArmingStatus_returnChangedArmingStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(repository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }**/

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
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.handleSensorActivated();
    }
    @Test
    void handleSensorDeactivatedTest_systemSetToAlarm_runsSpecificBranchInHandleSensorDeactivatedMethod()
    {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.handleSensorDeactivated();
        verify(repository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        lenient().when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.handleSensorDeactivated();
    }
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY", "DISARMED"})
    void setArmingStatusTest(ArmingStatus armingStatus)
    {
        repository.setArmingStatus(ArmingStatus.DISARMED);
       when(repository.getArmingStatus()).thenReturn(armingStatus);
       securityService.setArmingStatus(ArmingStatus.DISARMED);
       lenient().when(repository.getCatStatus()).thenReturn(true);
       lenient().when(securityService.saveArmingStatus()).thenReturn(ArmingStatus.DISARMED);
       securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(repository).setArmingStatus(ArmingStatus.ARMED_HOME);
        sensor.setActive(false);
    }
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setArmingStatusTestTwo(ArmingStatus armingStatus) {
        Sensor sensor1 = new Sensor("Living Room", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Back door", SensorType.DOOR);
        repository.addSensor(sensor1);
        repository.addSensor(sensor2);
        sensor1.setActive(false);
        sensor2.setActive(false);
        securityService.setArmingStatus(armingStatus);
        sensorMock.setActive(false);
    }

    @Test
    void catDetectedTest()
    {

            repository.setCatStatus(false);
            lenient().when(repository.getCatStatus()).thenReturn(false);
            lenient().when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

              lenient().when(repository.getCatStatus()).thenReturn(true); //cause catStat to get hit with true
              repository.setArmingStatus(ArmingStatus.ARMED_HOME);
             lenient().when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
             securityService.catDetected(true);

             lenient().when(repository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
            securityService.catDetected(true);

            lenient().when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
            securityService.catDetected(true);



            lenient().when(repository.getCatStatus()).thenReturn(false);
            lenient().when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        securityService.catDetected(false);
    }
    @Test
    void catDetectedPartTwo()
    {
        lenient().when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        Set<Sensor> sensors = new HashSet<>(2);
        Sensor aSensor = new Sensor("front door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("back door", SensorType.DOOR);
        sensors.add(aSensor);
        sensors.add(sensor2);
        aSensor.setActive(true);
        when(repository.getSensors()).thenReturn(sensors);
        securityService.catDetected(false);

        lenient().when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        Set<Sensor> sensorsSet2 = new HashSet<>(2);
        Sensor sensor3 = new Sensor("back door", SensorType.DOOR);
        sensors.add(sensor3);
        sensor2.setActive(false);
        when(repository.getSensors()).thenReturn(sensorsSet2);
        securityService.catDetected(false);}
    @Test
    void resetSensorsTest()
    {
        Set<Sensor> sensors = new HashSet<>(2);
        Sensor aSensor = new Sensor("front door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("back door", SensorType.DOOR);
        sensors.add(aSensor);
        sensors.add(sensor2);
        aSensor.setActive(true);
        sensor2.setActive(true);
        securityService.resetSensors(sensors);
        lenient().when(securityServiceMock.resetSensors(sensors)).thenReturn(sensors);
    }

}
