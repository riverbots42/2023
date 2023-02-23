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
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import edu.wpi.first.wpilibj.Joystick;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;


import edu.wpi.first.wpilibj.Encoder;
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
    
  // Initializes an encoder on DIO pins 0 and 1
  // Defaults to 4X decoding and non-inverted
  Encoder encoder = new Encoder(0, 1);
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

    //Encoder's distance/pulse
    encoder.setDistancePerPulse(1./256.);
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

  @Override
  public void autonomousInit() {
    // schedule the autonomous command (example)
    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
    //set robot starting distance to 0
    encoder.reset();
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() 
  {
    //If 10 feet haven't been travelled, move forward
    if(encoder.getDistance() < 10)
    {

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
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {

    double RightTriggerOut = stick.getRawAxis(RIGHT_TRIGGER);
    double LeftTriggerOut = stick.getRawAxis(LEFT_TRIGGER);

    //These all connect to seperate motors and actually control the output.  (Makes wheels, screwdrive, ect, GO)
    //Multiplied by 65% to decrease power and make the robot more controllable
    leftMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)*0.65);
    leftMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)*0.65);
    rightMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL)*0.65);
    rightMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL)*0.65);
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
    
    
    //Should probably be replaced with a timer (Accelerometer returns differing values while stationary)
    double previousXAccelerometer = accelerometer.getX();
    double previousYAccelerometer = accelerometer.getY();
    double previousZAccelerometer = accelerometer.getZ();
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