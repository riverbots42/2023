package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;

public class Path
{
    //these may need to be floats depending on the encoder, I'll just wait and see though
    double leftTarget;
    double rightTarget;
    double wait;
    VictorSPX l_1, l_2, r_1, r_2;
    Encoder l_enc, r_enc;

    public Path(double leftTarget, double rightTarget, double wait, VictorSPX l_1, VictorSPX l_2, VictorSPX r_1, VictorSPX r_2, Encoder l_e, Encoder r_e)
    {
        this.leftTarget = leftTarget;
        this.rightTarget = rightTarget;
        this.wait = wait;
        this.l_1 = l_1;
        this.l_2 = l_2;
        this.r_1 = r_1;
        this.r_2 = r_2;
        this.l_enc = l_e;
        this.r_enc = r_e;
    }
    /*
    tick() is run every 20ms and:
    Measures distance we've actually travelled.
    Sets the motor to some reasonable rate (50%, 10%, etc) depending on how far away we are from target.
    If wait > 0, we may just decrement wait.
    */
    public void tick() {
        double curLeft = l_enc.getDistance();
        double curRight = r_enc.getDistance();
        System.out.printf("%f <-> %f = %f, %f <-> %f = %f\n", curLeft, leftTarget, curLeft - leftTarget, curRight, rightTarget, curRight - rightTarget);
        if(curLeft - leftTarget < -0.5) {
            // leftTarget is positive
            // We have a long way to go to get to leftTarget (leftTarget >> curLeft)
            l_1.set(ControlMode.PercentOutput, -.6);
            l_2.set(ControlMode.PercentOutput, -.6);
        } else if(curLeft - leftTarget > +0.5) {
            // leftTarget is negative
            // We have a long way to go to get to leftTarget (leftTarget << curLeft)
            l_1.set(ControlMode.PercentOutput, +.6);
            l_2.set(ControlMode.PercentOutput, +.6);
        } else if(curLeft - leftTarget < -0.1 ) {
            // leftTarget is positive, but we're close
            // We have a short way to go to get to leftTarget (leftTarget > curLeft)
            l_1.set(ControlMode.PercentOutput, -.1);
            l_2.set(ControlMode.PercentOutput, -.1);
        } else if(curLeft - leftTarget > +0.1) {
            // leftTarget is negative
            // We have a short way to go to get to leftTarget (leftTarget < curLeft)
            l_1.set(ControlMode.PercentOutput, +.1);
            l_2.set(ControlMode.PercentOutput, +.1);
        }
        if(curRight - rightTarget < -0.5) {
            // rightTarget is positive
            // We have a long way to go to get to rightTarget (rightTarget >> curRight)
            r_1.set(ControlMode.PercentOutput, -.6);
            r_2.set(ControlMode.PercentOutput, -.6);
        } else if(curRight - rightTarget > +0.5) {
            // rightTarget is negative
            // We have a long way to go to get to rightTarget (rightTarget << curRight)
            r_1.set(ControlMode.PercentOutput, +.6);
            r_2.set(ControlMode.PercentOutput, +.6);
        } else if(curRight - rightTarget < -0.1 ) {
            // rightTarget is positive, but we're close
            // We have a short way to go to get to rightTarget (rightTarget > curRight)
            r_1.set(ControlMode.PercentOutput, -.1);
            r_2.set(ControlMode.PercentOutput, -.1);
        } else if(curRight - rightTarget > +0.1) {
            // rightTarget is negative
            // We have a short way to go to get to rightTarget (rightTarget < curRight)
            r_1.set(ControlMode.PercentOutput, +.1);
            r_2.set(ControlMode.PercentOutput, +.1);
        }
    }
    //Since we're only going forward and backwards, we can just use the left encoder to get distance.
    //If we implement turns, we'll have to use both
    public boolean correctDistance()
    {
        double leftDelta = l_enc.getDistance() - leftTarget;
        double rightDelta = r_enc.getDistance() - rightTarget;

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
