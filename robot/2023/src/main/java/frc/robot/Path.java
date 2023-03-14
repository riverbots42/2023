package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import edu.wpi.first.wpilibj.Encoder;

public class Path{
    //these may need to be floats depending on the encoder, I'll just wait and see though
    double leftTarget;
    double rightTarget;
    double wait;
    VictorSPX leftVictor1, leftVictor2, rightVictor1, rightVictor2;
    Encoder leftEncoder, rightEncoder;
    final double AUTONOMOUS_ROBOT_SPEED = .8;
    final double READJUSTMENT_SPEED  = .6;

    public Path(double leftTarget, double rightTarget, double wait, VictorSPX leftVictor1, VictorSPX leftVictor2, VictorSPX rightVictor1, VictorSPX rightVictor2, Encoder leftEncoder, Encoder rightEncoder)
    {
        this.leftTarget = leftTarget;
        this.rightTarget = rightTarget;
        this.wait = wait;
        this.leftVictor1 = leftVictor1;
        this.leftVictor2 = leftVictor2;
        this.rightVictor1 = rightVictor1;
        this.rightVictor2 = rightVictor2;
        this.leftEncoder = leftEncoder;
        this.rightEncoder = rightEncoder;
    }
    /*
    tick() is run every 20ms and:
    Measures distance we've actually travelled.
    Sets the motor to some reasonable rate (50%, 10%, etc) depending on how far away we are from target.
    If wait > 0, we may just decrement wait.
    */
    public void tick() 
    {
        double curLeft = leftEncoder.getDistance();
        double curRight = rightEncoder.getDistance();
        System.out.printf("%f <-> %f = %f, %f <-> %f = %f\n", curLeft, leftTarget, curLeft - leftTarget, curRight, rightTarget, curRight - rightTarget);
        if(curLeft - leftTarget < -0.5) 
        {
            // leftTarget is positive
            // We have a long way to go to get to leftTarget (leftTarget >> curLeft)
            leftVictor1.set(ControlMode.PercentOutput, - AUTONOMOUS_ROBOT_SPEED);
            leftVictor2.set(ControlMode.PercentOutput, - AUTONOMOUS_ROBOT_SPEED);
        } 
        else if(curLeft - leftTarget > +0.5) 
        {
            // leftTarget is negative
            // We have a long way to go to get to leftTarget (leftTarget << curLeft)
            leftVictor1.set(ControlMode.PercentOutput, + AUTONOMOUS_ROBOT_SPEED);
            leftVictor2.set(ControlMode.PercentOutput, + AUTONOMOUS_ROBOT_SPEED);
        } 
        else if(curLeft - leftTarget < -0.1 ) 
        {
            // leftTarget is positive, but we're close
            // We have a short way to go to get to leftTarget (leftTarget > curLeft)
            leftVictor1.set(ControlMode.PercentOutput, - READJUSTMENT_SPEED);
            leftVictor2.set(ControlMode.PercentOutput, - READJUSTMENT_SPEED);
        } 
        else if(curLeft - leftTarget > +0.1) 
        {
            // leftTarget is negative
            // We have a short way to go to get to leftTarget (leftTarget < curLeft)
            leftVictor1.set(ControlMode.PercentOutput, + READJUSTMENT_SPEED);
            leftVictor2.set(ControlMode.PercentOutput, + READJUSTMENT_SPEED);
        }
        if(curRight - rightTarget < -0.5) 
        {
            // rightTarget is positive
            // We have a long way to go to get to rightTarget (rightTarget >> curRight)
            rightVictor1.set(ControlMode.PercentOutput, - AUTONOMOUS_ROBOT_SPEED);
            rightVictor2.set(ControlMode.PercentOutput, - AUTONOMOUS_ROBOT_SPEED);
        } 
        else if(curRight - rightTarget > +0.5) 
        {
            // rightTarget is negative
            // We have a long way to go to get to rightTarget (rightTarget << curRight)
            rightVictor1.set(ControlMode.PercentOutput, + AUTONOMOUS_ROBOT_SPEED);
            rightVictor2.set(ControlMode.PercentOutput, + AUTONOMOUS_ROBOT_SPEED);
        } 
        else if(curRight - rightTarget < -0.1 ) 
        {
            // rightTarget is positive, but we're close
            // We have a short way to go to get to rightTarget (rightTarget > curRight)
            rightVictor1.set(ControlMode.PercentOutput, - READJUSTMENT_SPEED);
            rightVictor2.set(ControlMode.PercentOutput, - READJUSTMENT_SPEED);
        } 
        else if(curRight - rightTarget > +0.1) 
        {
            // rightTarget is negative
            // We have a short way to go to get to rightTarget (rightTarget < curRight)
            rightVictor1.set(ControlMode.PercentOutput, + READJUSTMENT_SPEED);
            rightVictor2.set(ControlMode.PercentOutput, + READJUSTMENT_SPEED);
        }
    }
    //Since we're only going forward and backwards, we can just use the left encoder to get distance.
    //If we implement turns, we'll have to use both
    public boolean correctDistance()
    {
        double leftDelta = leftEncoder.getDistance() - leftTarget;
        double rightDelta = rightEncoder.getDistance() - rightTarget;

        if(leftDelta >= -0.1 && leftDelta <= 0.1)
        {
            if(rightDelta >= -0.1 && rightDelta <= 0.1)
            {
               return true;
            }
        }
        return false;
    }
    public boolean isDone() 
    {
        if(correctDistance() == true && wait <= 0)
            return true;
        else
            return false;
    }
}