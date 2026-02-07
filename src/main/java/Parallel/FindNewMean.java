package Parallel;

import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.WasteSite;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FindNewMean implements Runnable{
    private final Cluster start_Cluster;
    private final List<WasteSite> wasteSiteList;
    private final CountDownLatch latch;

    public FindNewMean(Cluster start_Cluster, CountDownLatch latch){
        this.start_Cluster = start_Cluster;
        this.wasteSiteList = start_Cluster.getWasteSiteList();
        this.latch = latch;
    }
    @Override
    public void run() {
        if(calculateMedian()){
            Parallel.flag.set(true);
        }
        //System.out.println("Found mean for " + Thread.currentThread().getName());
        latch.countDown();
    }
    private boolean calculateMedian(){
        boolean change = false;
        if(!wasteSiteList.isEmpty()) {

            double[] newCenter = start_Cluster.changeCenter();

            double distance = EuclideanDistance.calculate(
                    start_Cluster.getLa(), start_Cluster.getLo(), newCenter[0], newCenter[1]);

            if (distance > 0.001) {
                start_Cluster.setLa(newCenter[0]);
                start_Cluster.setLo(newCenter[1]);
                change = true;
            }
        }

        return change;
    }
}
