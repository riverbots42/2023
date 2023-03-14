// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.interfaces.Accelerometer;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import edu.wpi.first.wpilibj.BuiltInAccelerometer;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import edu.wpi.first.wpilibj.Joystick;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;


import edu.wpi.first.wpilibj.Encoder;
import java.util.*;
/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  // private Command m_autonomousCommand;

  private final Joystick stick = new Joystick(0);
  VictorSPX leftMotorControllerOne = new VictorSPX(5);
  VictorSPX leftMotorControllerTwo = new VictorSPX(6);
  VictorSPX rightMotorControllerOne = new VictorSPX(7);
  VictorSPX rightMotorControllerTwo = new VictorSPX(8);
  //PWM channel 0 is broken on our current RoboRio.  Would not recommend trying to use it
  Spark brushElevator = new Spark(3);
  Spark screwDriveMotor = new Spark(2);

  static final Port onBoard = Port.kOnboard;
  static final int gyroAdress = 0x68;
  I2C gyro;
  Accelerometer accelerometer = new BuiltInAccelerometer();
  // Initializes an encoder on DIO pins 0 and 1
  // Defaults to 4X decoding and non-inverted
  Encoder leftEncoder = new Encoder(0, 1);
  Encoder rightEncoder = new Encoder(2,3);
  Iterator<Path> pathElements;
  Path currentPath;
  final double ENCODER_DISTANCE_PER_PULSE = 34.29/256.;


  //These constants set axes and channels for the controller. The first two are axes. 
  //On the back of the Logitech controller we use, there is a switch.
  //Ensure the switch it set to "X" rather than "D" or the channels will be wrong
  
  final int LEFT_STICK_VERTICAL = 1;
  final int RIGHT_STICK_VERTICAL = 5;
  final int LEFT_TRIGGER = 2;
  final int RIGHT_TRIGGER = 3;
  final int LEFT_BUMPER = 5;
  final int RIGHT_BUMPER = 6;
  final int BRAKE_BUTTON_B = 2;
  final int NITRO_BUTTON_X = 3;
  double robotSpeedMultiplier = .65;
  boolean isTurbo = false;

  //For pathing, in cm 
  //MAY NEED FURTHER CALIBRATION
  //Is distance from edge of scoring mechanism to middle of charging station +  length of verticle travel on charging station
  //minus distance from back to middle of robot 
  final double ROBOT_TO_PLATFORM_PATH_TWO = 244.105;
  
  UsbCamera parkingCamera;
  UsbCamera leftBackCamera;
  UsbCamera rightBackCamera;
  NetworkTableEntry camera;

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() 
  {
    // Instantiate our RobotContainer.  This will perform all our button bindings, and put our
    // autonomous chooser on the dashboard.

    camera = NetworkTableInstance.getDefault().getTable("").getEntry("CameraSelection");
    parkingCamera = CameraServer.startAutomaticCapture(0);
    leftBackCamera = CameraServer.startAutomaticCapture(1);
    rightBackCamera = CameraServer.startAutomaticCapture(2);

    //Encoder's distance/pulse
    leftEncoder.setDistancePerPulse(ENCODER_DISTANCE_PER_PULSE);
    rightEncoder.setDistancePerPulse(ENCODER_DISTANCE_PER_PULSE);
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() 
  {
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

  @Override
  public void autonomousInit() 
  {
    //set robot starting distance to 0
    leftEncoder.reset();
    leftEncoder.setReverseDirection(true);
    rightEncoder.reset();
    leftMotorControllerOne.setInverted(true);
    leftMotorControllerTwo.setInverted(true);
    ArrayList<Path> pathArray = new ArrayList<Path>();
    pathArray.add(new Path(ROBOT_TO_PLATFORM_PATH_TWO, ROBOT_TO_PLATFORM_PATH_TWO, 0, leftMotorControllerOne, leftMotorControllerTwo, rightMotorControllerOne, rightMotorControllerTwo, leftEncoder, rightEncoder));
    pathElements = pathArray.iterator(); 
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic()
  {
    System.out.printf("%f %f\n", leftEncoder.getDistance(), rightEncoder.getDistance());
    if(currentPath == null && pathElements.hasNext())
    {
      currentPath = pathElements.next();
    }
    if(currentPath != null) 
    {
      if(currentPath.isDone())
      {
        currentPath = null;
        Stop();
      }
      if(currentPath != null)
        currentPath.tick();
      else
        Stop();
    }
  }

  @Override
  public void teleopInit() 
  {
    stick.setXChannel(LEFT_STICK_VERTICAL);
    stick.setYChannel(RIGHT_STICK_VERTICAL);
    //Left side needs to be inversed to go forwards, otherwise it will work against the right side. (Robot will spin)
    leftMotorControllerOne.setInverted(true);
    leftMotorControllerTwo.setInverted(true);
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() 
  {
    double RightTriggerOut = stick.getRawAxis(RIGHT_TRIGGER) * .50;
    double LeftTriggerOut = stick.getRawAxis(LEFT_TRIGGER) * .50;
    
    if(stick.getRawButtonPressed(NITRO_BUTTON_X))
    {
      isTurbo = !isTurbo;
    }
    if(isTurbo)
    {
      robotSpeedMultiplier = 1;
    }
    else
    {
      robotSpeedMultiplier = 0.75;
    }
    
    //These all connect to seperate motors and actually control the output.  (Makes wheels, screwdrive, ect, GO)
    leftMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)* robotSpeedMultiplier);
    leftMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)* robotSpeedMultiplier);
    rightMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL)* robotSpeedMultiplier);
    rightMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL)* robotSpeedMultiplier);
    brushElevator.set(RightTriggerOut - LeftTriggerOut);

    //if right bumper is pressed, screw drive goes one direction, if left is pressed, it goes the other.  Otherwise, remain
    //stationary
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
 
     if(stick.getRawButton(BRAKE_BUTTON_B))
     {
      leftMotorControllerOne.setNeutralMode(NeutralMode.Brake);
      leftMotorControllerTwo.setNeutralMode(NeutralMode.Brake);
      rightMotorControllerOne.setNeutralMode(NeutralMode.Brake);
      rightMotorControllerTwo.setNeutralMode(NeutralMode.Brake);
     }
    else
    {
      leftMotorControllerOne.setNeutralMode(NeutralMode.Coast);
      leftMotorControllerTwo.setNeutralMode(NeutralMode.Coast);
      rightMotorControllerOne.setNeutralMode(NeutralMode.Coast);
      rightMotorControllerTwo.setNeutralMode(NeutralMode.Coast);
    }
  }
  
  @Override
  public void testInit() 
  {
    // Cancels all running commands at the start of test mode.
    CommandScheduler.getInstance().cancelAll();
  }
  
  public void Stop()
  {
    leftMotorControllerOne.set(ControlMode.PercentOutput, 0);
    leftMotorControllerTwo.set(ControlMode.PercentOutput, 0);
    rightMotorControllerOne.set(ControlMode.PercentOutput, 0);
    rightMotorControllerTwo.set(ControlMode.PercentOutput, 0);
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