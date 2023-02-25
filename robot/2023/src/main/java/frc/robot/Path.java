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
        bot.encoder.getDistance();
        // set something
    }
    public boolean isDone() {
        if(bot.encoder.getDistance() >= 0.1 && bot.encoder.getDistance() <= 0.1)
            return true;
        else
            return false;
    }
}
