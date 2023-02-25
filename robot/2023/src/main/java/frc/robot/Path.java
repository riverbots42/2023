package frc.robot;

public class Path extends Robot
{
    //these may need to be floats depending on the encoder, I'll just wait and see though
    double target;
    double wait;
    Robot bot;
    public Path(double target, double wait, Robot bot)
    {
        this.target = target;
        this.wait = wait;
        this.bot = bot;
    }
    public void tick() {
        bot.leftEncoder.getDistance();
        bot.rightEncoder.getDistance();
        // set something
    }
    //Since we're only going forward and backwards, we can just use the left encoder to get distance.
    //If we implement turns, we'll have to use both
    public boolean isDone() {
        if(bot.leftEncoder.getDistance() >= 0.1 && bot.leftEncoder.getDistance() <= 0.1)
            return true;
        else
            return false;
    }
}
