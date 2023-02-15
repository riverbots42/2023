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

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;



/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private Command m_autonomousCommand;
  
  AddressableLED leds = new AddressableLED(3);
  AddressableLEDBuffer ledBuff = new AddressableLEDBuffer(60);

  private final Joystick stick = new Joystick(0);
  VictorSPX leftMotorControllerOne = new VictorSPX(5);
  VictorSPX leftMotorControllerTwo = new VictorSPX(6);
  VictorSPX rightMotorControllerOne = new VictorSPX(7);
  VictorSPX rightMotorControllerTwo = new VictorSPX(8);
  Spark sparkScoringMechanismMotor = new Spark(1);
  Spark screwDriveMotor = new Spark(2);

  private RobotContainer m_robotContainer;

  static final Port onBoard = Port.kOnboard;
  static final int gyroAdress = 0x68;
  I2C gyro;
  Accelerometer accelerometer = new BuiltInAccelerometer();
  //These constants set channels for the controller.  On the back of the Logitech controller we use,
  //there is a switch.  Ensure the switch it set to "X" rather than "D" or the channels will be wrong
  final int LEFT_STICK_VERTICAL = 1;
  final int RIGHT_STICK_VERTICAL = 5;
  final int RIGHT_STICK_HORIZONTAL = 4;
  final int LEFT_TRIGGER = 2;
  final int RIGHT_TRIGGER = 3;
  final int LEFT_BUMPER = 10;
  final int RIGHT_BUMPER = 11;
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

    
    leds.setLength(ledBuff.getLength());
    leds.setData(ledBuff);
    leds.start();

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

    double RightTriggerOut = stick.getRawAxis(RIGHT_TRIGGER);
    double LeftTriggerOut = stick.getRawAxis(LEFT_TRIGGER);
    double RightBumperOut = stick.getRawAxis(RIGHT_BUMPER);
    double LeftBumperOut = stick.getRawAxis(LEFT_BUMPER);
    
    double prevMotorPercent = stick.getRawAxis(LEFT_STICK_VERTICAL);

    leftMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL) * 0.3);
    leftMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)* 0.3);
    rightMotorControllerOne.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL) * 0.3);
    rightMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,stick.getRawAxis(LEFT_STICK_VERTICAL)* 0.3);
    double nowMotorPercent = stick.getRawAxis(LEFT_STICK_VERTICAL);
    if(prevMotorPercent != nowMotorPercent)
    {
      leftMotorControllerOne.set(VictorSPXControlMode.PercentOutput,prevMotorPercent + ((RIGHT_STICK_HORIZONTAL) * -0.5));
      leftMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,prevMotorPercent + ((RIGHT_STICK_HORIZONTAL) * -0.5));
      rightMotorControllerOne.set(VictorSPXControlMode.PercentOutput,prevMotorPercent + ((RIGHT_STICK_HORIZONTAL) * -0.5));
      rightMotorControllerTwo.set(VictorSPXControlMode.PercentOutput,prevMotorPercent + ((RIGHT_STICK_HORIZONTAL) * -0.5));
    }

    sparkScoringMechanismMotor.set(RightTriggerOut - LeftTriggerOut);
    screwDriveMotor.set(RightBumperOut - LeftBumperOut);
    //and should probably be replaced with a timer-
    /* 
    int choice = (int)(Math.random()*3);
    if(choice == 0)
    {
      for(int i = 0; i < ledBuff.getLength(); i++)
      {
        ledBuff.setRGB(i,166,16,30);
      }
    }
    else if(choice == 1)
    {
      for(int i = 0; i < ledBuff.getLength(); i++)
      {
        ledBuff.setRGB(i,255,105,180);
      }
    }
    else
    {
      for(int i = 0; i < ledBuff.getLength(); i++)
      {
        ledBuff.setRGB(i,255,192,203);
      }
    }
    */
    for(int i = 0; i < ledBuff.getLength(); i++)
    {
      ledBuff.setRGB(i,255,255,203);
      System.out.println("PLZ HELP");
    }
    
    

    // Blood red == 166,16,30
    // Hot pink == 255,105,180
    // Pink == 255,192,203
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