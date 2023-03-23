package frc.robot;

import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import java.util.ArrayList;
import com.kauailabs.navx.frc.AHRS;
public class AutoBalance 
{
    AHRS navX;
    static final double kOffBalanceAngleThresholdDegrees = 10;
    static final double kOonBalanceAngleThresholdDegrees  = 5;
    public static double avgAdjustRate = 0;
    static ArrayList<Double> pitchList = new ArrayList<Double>();
    static double ltotal = 0;
    
    
    public void updateMotor(VictorSPX leftVictor1, VictorSPX leftVictor2, VictorSPX rightVictor1, VictorSPX rightVictor2)
    {
        leftVictor1.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.3);
        leftVictor2.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.3);
        rightVictor1.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.3);
        rightVictor2.set(VictorSPXControlMode.PercentOutput,avgAdjustRate * 0.3);
    }
    public void updateList()
    {
        double pitchAngleDegrees = navX.getPitch();
        double pitchAngleRadians = pitchAngleDegrees * (Math.PI / 180.0);
        ltotal += pitchAngleRadians;
        pitchList.add(pitchAngleDegrees);
        
        if(pitchList.size() > 50)
        {
            ltotal -= pitchList.get(0);
            pitchList.remove(0);
        
        }
        avgAdjustRate = ltotal / pitchList.size();
    }
    
    
    public boolean checkPitch()
    {
        double pitchAngleDegrees = navX.getPitch();
        if ((Math.abs(pitchAngleDegrees) >= Math.abs(kOffBalanceAngleThresholdDegrees))) 
        {
            return true;
        }
            // Check if it is less than 5 degrees off.
        else if ((Math.abs(pitchAngleDegrees) <= Math.abs(kOonBalanceAngleThresholdDegrees))) 
        {
            return false;
        }
        return false;
    }
}
