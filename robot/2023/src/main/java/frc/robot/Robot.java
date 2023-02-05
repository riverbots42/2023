// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.interfaces.Accelerometer;
import edu.wpi.first.wpilibj.BuiltInAccelerometer;


import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import edu.wpi.first.wpilibj.Joystick;
import com.revrobotics.CANSparkMax;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;


/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private Command m_autonomousCommand;

  private final Joystick stick = new Joystick(0);
  //Still need tested to see which motor controllers are which
  VictorSPX leftMotorControllerOne = new VictorSPX(5);
  VictorSPX leftMotorControllerTwo = new VictorSPX(6);
  VictorSPX rightMotorControllerOne = new VictorSPX(7);
  VictorSPX rightMotorControllerTwo = new VictorSPX(8);
  //Screw drive will no longer be a VictorSPX, shift to new Talon
  VictorSPX screwDriveMotorController = new VictorSPX(3);
  private RobotContainer m_robotContainer;

  static final Port onBoard = Port.kOnboard;
  static final int gyroAdress = 0x68;
  I2C gyro;
  Accelerometer accelerometer = new BuiltInAccelerometer();
  
  final int LEFT_STICK_VERTICAL = 1;
  final int RIGHT_STICK_VERTICAL = 3;
  final int LEFT_TRIGGER = 2;
  final int RIGHT_TRIGGER = 3;

  UsbCamera parkingCamera;
  NetworkTableEntry camera;

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    // Instantiate our RobotContainer.  This will perform all our button bindings, and put our
    // autonomous chooser on the dashboard.
    m_robotContainer = new RobotContainer();

    camera = NetworkTableInstance.getDefault().getTable("").getEntry("CameraSelection");
    parkingCamera = CameraServer.startAutomaticCapture(0);
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
    m_autonomousCommand = m_robotContainer.getAutonomousCommand();

    // schedule the autonomous command (example)
    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {}

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

    double RightTriggerOut = stick.getRawAxis(RIGHT_TRIGGER) * 0.8;
    double LeftTriggerOut = stick.getRawAxis(LEFT_TRIGGER) * 0.8;

    leftMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL));
    leftMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL));
    rightMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL));
    rightMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(RIGHT_STICK_VERTICAL));
    screwDriveMotorController.set(VictorSPXControlMode.PercentOutput, RightTriggerOut - LeftTriggerOut);
    
    //Updates console whenever a value in accelerometer changes, though it's still too fast
    //and should probably be replaced with a timer
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