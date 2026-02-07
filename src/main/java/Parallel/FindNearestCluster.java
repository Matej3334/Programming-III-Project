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
    private final Cluster sCluster;
    private final CountDownLatch latch;

    public FindNearestCluster(Cluster sCluster, List<Cluster> clusterList, CountDownLatch latch){
        this.clusterList = clusterList;
        this.sCluster = sCluster;
        this.wasteSiteList = sCluster.getWasteSiteList();
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
            nearestCluster.addWasteSiteParallel(wasteSite);

        }
        latch.countDown();
        //System.out.println(Thread.currentThread().getName() + " waste sites done");
    }
}
