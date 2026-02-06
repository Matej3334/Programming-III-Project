package Parallel;

import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.WasteSite;


import java.util.List;
import java.util.concurrent.CountDownLatch;


public class FindNearestCluster implements Runnable{
    private final List<WasteSite> wasteSiteList;
    private final List<Cluster> clusterList;
    private final Cluster start_Cluster;
    private final CountDownLatch latch;

    public FindNearestCluster(Cluster start_Cluster, List<Cluster> clusterList, CountDownLatch latch){
        this.clusterList = clusterList;
        this.start_Cluster = start_Cluster;
        this.wasteSiteList = start_Cluster.getWasteSiteListCopy();
        this.latch = latch;
    }

    @Override
    public void run() {

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

            if(nearestCluster != start_Cluster){
                nearestCluster.addWasteSiteParallel(wasteSite);
                start_Cluster.removeWasteSiteParallel(wasteSite);
                System.out.println(Thread.currentThread().getName() + " changed site" );
            }
        }

        latch.countDown();
        System.out.println(Thread.currentThread().getName() + " waste sites done");
    }
}
