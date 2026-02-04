package Parallel;

import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.WasteSite;


import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

public class FindNearestCluster implements Runnable{
    private List<WasteSite> wasteSiteList;
    private final List<Cluster> clusterList;
    private final Cluster start_Cluster;
    public static CyclicBarrier barrier;
    public FindNearestCluster(Cluster start_Cluster, List<Cluster> clusterList, CyclicBarrier barrier){
        this.clusterList = clusterList;
        this.start_Cluster = start_Cluster;
        this.wasteSiteList = start_Cluster.getWasteSiteList();
        FindNearestCluster.barrier = barrier;
    }

    @Override
    public void run() {
        do {
            Parallel.changeFlag.set(false);

            if(calculateMedian()){
                Parallel.changeFlag.set(true);
            }

            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            for (WasteSite wasteSite : wasteSiteList) {
                Cluster nearestCluster = start_Cluster;
                double minDistance = EuclideanDistance.calculate(wasteSite.la(), wasteSite.lo(), start_Cluster.getLa(), start_Cluster.getLo());

                for (Cluster cluster : clusterList) {
                    double distance = EuclideanDistance.calculate(
                            wasteSite.la(), wasteSite.lo(), cluster.getLa(), cluster.getLo());

                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestCluster = cluster;
                    }
                }
            }
        } while (Parallel.changeFlag.get());
    }

    private boolean calculateMedian(){
        boolean change = false;
        if(!start_Cluster.getWasteSiteList().isEmpty()) {

            double[] newCenter = start_Cluster.changeCenter();

            double distance = EuclideanDistance.calculate(
                        start_Cluster.getLa(), start_Cluster.getLo(), newCenter[0], newCenter[1]);

            if (distance > 0.01) {
                start_Cluster.setLa(newCenter[0]);
                start_Cluster.setLo(newCenter[1]);
                change = true;
            }
        }

        return change;
    }
}
