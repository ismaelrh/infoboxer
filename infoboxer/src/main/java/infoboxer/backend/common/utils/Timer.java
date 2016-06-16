package infoboxer.backend.common.utils;

/**
 * Created by ismaro3 on 26/08/15.
 * Used for counting time.
 */
public class Timer {


    public Timer(){};


    private String message;
    private double startTime;
    private boolean verbose;
    public void start(String message, boolean verbose){
        this.message = message;
        startTime = System.nanoTime();
        this.verbose = verbose;
    }

    public void start(String message){
        this.start(message,true);
    }

    public double stop(){
        double elapsed = System.nanoTime() - startTime;
        double elapsedSec = (double)elapsed / 1000000000.0;
        if(verbose){
            System.out.println("[TIMER] " + message + " took " + elapsedSec + "s.s");
        }

        return elapsedSec;
    }

}
