package Parallel;

import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.WasteSite;


import java.util.List;
import java.util.concurrent.CountDownLatch;

//THIS IS NOT USEDDDDDDDDDDDDDDDDDDDDDDDDDD

public class FindNearestCluster implements Runnable{
    private final List<WasteSite> wasteSiteList;
    private final List<Cluster> clusterList;
    private final Cluster start_Cluster;
    private final CountDownLatch latch;

    public FindNearestCluster(Cluster start_Cluster, List<Cluster> clusterList, CountDownLatch latch){
        this.clusterList = clusterList;
        this.start_Cluster = start_Cluster;
        this.wasteSiteList = start_Cluster.getWasteSiteList();
        this.latch = latch;
    }

    @Override
    public void run() {

        for (WasteSite wasteSite : wasteSiteList) {
            Cluster nearestCluster = null;
            double minDistance = Double.MAX_VALUE;

            for(Cluster cluster : clusterList){
                double distance = EuclideanDistance.calculate(
                        wasteSite.la(), wasteSite.lo(), cluster.getLa(), cluster.getLo());

                if(distance < minDistance){
                    minDistance = distance;
                    nearestCluster = cluster;
                }
            }

            assert nearestCluster != null;
            if(nearestCluster != start_Cluster){
                nearestCluster.addWasteSiteParallel(wasteSite);
                start_Cluster.removeWasteSiteParallel(wasteSite);
                System.out.println(Thread.currentThread().getName() + " changed site" );
            } else {
                start_Cluster.addWasteSiteParallel(wasteSite);
            }
        }

        latch.countDown();
        System.out.println(Thread.currentThread().getName() + " waste sites done");
    }
}
