// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.I2C.Port;

import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.interfaces.Accelerometer;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import edu.wpi.first.wpilibj.BuiltInAccelerometer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import edu.wpi.first.wpilibj.Joystick;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.ArrayList;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private Command m_autonomousCommand;

  private final Joystick stick = new Joystick(0);
  VictorSPX leftMotorControllerOne = new VictorSPX(5);
  VictorSPX leftMotorControllerTwo = new VictorSPX(6);
  VictorSPX rightMotorControllerOne = new VictorSPX(7);
  VictorSPX rightMotorControllerTwo = new VictorSPX(8);
  //PWM channel 0 is broken on our current RoboRio.  Would not recommend trying to use it
  Spark brushElevator = new Spark(1);
  Spark screwDriveMotor = new Spark(2);

  static final Port onBoard = Port.kOnboard;
  static final int gyroAdress = 0x68;
  I2C gyro;
  
  Accelerometer accelerometer = new BuiltInAccelerometer();
  //These constants set axes and channels for the controller. The first two are axes. 
  //On the back of the Logitech controller we use, there is a switch.
  //Ensure the switch it set to "X" rather than "D" or the channels will be wrong
  
  final int LEFT_STICK_VERTICAL = 1;
  final int RIGHT_STICK_VERTICAL = 5;

  final int LEFT_TRIGGER = 2;
  final int RIGHT_TRIGGER = 3;
  final int LEFT_BUMPER = 5;
  final int RIGHT_BUMPER = 6;
  UsbCamera parkingCamera;
  UsbCamera leftBackCamera;
  UsbCamera rightBackCamera;
  NetworkTableEntry camera;
  AHRS navX;
  static final double kOffBalanceAngleThresholdDegrees = 10;
  static final double kOonBalanceAngleThresholdDegrees  = 5;
  public double avgAdjustRate = 1;
  ArrayList<Double> number = new ArrayList<Double>();

  double ltotal = 0;
  

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    // Instantiate our RobotContainer.  This will perform all our button bindings, and put our
    // autonomous chooser on the dashboard.
    camera = NetworkTableInstance.getDefault().getTable("").getEntry("CameraSelection");
    parkingCamera = CameraServer.startAutomaticCapture(0);
    leftBackCamera = CameraServer.startAutomaticCapture(1);
    rightBackCamera = CameraServer.startAutomaticCapture(2);
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
    // Runs the Scheduler.  This is responsible for polling buttons, adding newly-scheduled
    // commands, running already-scheduled commands, removing finished or interrupted commands,
    // and running subsystem periodic() methods.  This must be called from the robot's periodic
    // block in order for anything in the Command-based framework to work.
    
    CommandScheduler.getInstance().run();
    
    
  }

  /** This function is called once each time the robot enters Disabled mode. */
  @Override
  public void disabledInit() {}


  @Override
  public void disabledPeriodic() {}

  /** This autonomous runs the autonomous command selected by your {@link RobotContainer} class. */
  @Override
  public void autonomousInit() {
    // schedule the autonomous command (example)
    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
    double pitchAngleDegrees = navX.getPitch();
    boolean autoBalancePitch = false;
    double pitchAngleRadians = pitchAngleDegrees * (Math.PI / 180.0);
    ltotal += pitchAngleRadians;
    number.add(pitchAngleDegrees);



    if(number.size() > 50)
    {
      ltotal -= number.get(0);
      number.remove(0);
        
    }
    avgAdjustRate = ltotal / 50;
    
    // Check if the pitch angle is more than the set threshold (10 degrees)
    if ( !autoBalancePitch && (Math.abs(pitchAngleDegrees) >= Math.abs(kOffBalanceAngleThresholdDegrees))) 
    {
      autoBalancePitch = true;
    }
    // Check if it is less than 5 degrees off.
    else if ( autoBalancePitch && (Math.abs(pitchAngleDegrees) <= Math.abs(kOonBalanceAngleThresholdDegrees))) 
    {
      autoBalancePitch = false;
    }
    
    if (autoBalancePitch) {
      
      leftMotorControllerOne.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.1);
      leftMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.1);
      rightMotorControllerOne.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.1);
      rightMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.1);
    }
  }

  @Override
  public void teleopInit() {
    // This makes sure that the autonomous stops running when
    // teleop starts running. If you want the autonomous to
    // continue until interrupted by another command, remove
    // this line or comment it out.
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }

    stick.setXChannel(LEFT_STICK_VERTICAL);
    stick.setYChannel(RIGHT_STICK_VERTICAL);
    //Left side needs to be inversed to go forwards, otherwise it will work against the right side. (Robot will spin)
    leftMotorControllerOne.setInverted(true);
    leftMotorControllerTwo.setInverted(true);

    gyro = new I2C(onBoard, gyroAdress);
    gyro.transaction(new byte[] {0x6B, 0x0}, 2, new byte[] {}, 0);
    gyro.transaction(new byte[] {0x1B, 0x10},  2, new byte[] {}, 0);
    System.out.println("debug plz");
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {

    double RightTriggerOut = stick.getRawAxis(RIGHT_TRIGGER);
    double LeftTriggerOut = stick.getRawAxis(LEFT_TRIGGER);

    //These all connect to seperate motors and actually control the output.  (Makes wheels, screwdrive, ect, GO)
    leftMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)*0.3);
    leftMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)*0.3);
    rightMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL)*0.3);
    rightMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL)*0.3);
    brushElevator.set(RightTriggerOut - LeftTriggerOut);

    if(stick.getRawButton(RIGHT_BUMPER))
    {
      screwDriveMotor.set(1);
    }
    else if(stick.getRawButton(LEFT_BUMPER))
    {
      screwDriveMotor.set(-1);
    }
    else
    {
      screwDriveMotor.set(0);
    }
    

    double previousXAccelerometer = accelerometer.getX();
    double previousYAccelerometer = accelerometer.getY();
    double previousZAccelerometer = accelerometer.getZ();
    //Should probably be replaced with a timer
    if(accelerometer.getX() != previousXAccelerometer)
    {
      System.out.println(accelerometer.getX());
    }
    if(accelerometer.getY() != previousYAccelerometer)
    {
      System.out.println(accelerometer.getY());
    }
    if(accelerometer.getZ() != previousZAccelerometer)
    {
      System.out.println(accelerometer.getZ());
    }
  }
  
  @Override
  public void testInit() {
    // Cancels all running commands at the start of test mode.
    CommandScheduler.getInstance().cancelAll();
  }

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}

  /** This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}
}