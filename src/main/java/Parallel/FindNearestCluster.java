package Parallel;

import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.WasteSite;


import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static Parallel.Parallel.iterations;

public class FindNearestCluster implements Runnable{
    private List<WasteSite> wasteSiteList;
    private final List<Cluster> clusterList;
    private final Cluster start_Cluster;
    public CyclicBarrier a_barrier;
    public CyclicBarrier m_barrier;
    public CyclicBarrier end_barrier;
    public FindNearestCluster(Cluster start_Cluster, List<Cluster> clusterList, CyclicBarrier
            assignment_barrier, CyclicBarrier median_barrier, CyclicBarrier end_barrier){
        this.clusterList = clusterList;
        this.start_Cluster = start_Cluster;
        this.wasteSiteList = start_Cluster.getWasteSiteListCopy();
        this.a_barrier = assignment_barrier;
        this.m_barrier = median_barrier;
        this.end_barrier = end_barrier;
    }

    @Override
    public void run() {

        System.out.println(Thread.currentThread().getName() + " starting for cluster at (" +
                start_Cluster.getLa() + ", " + start_Cluster.getLo() + ") with " +
                wasteSiteList.size() + " sites");

        calculateMedian();


        while (Parallel.flag.get() && iterations.get() <= 20){

            System.out.println(Thread.currentThread().getName() + " iteration " + iterations);

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

            System.out.println(Thread.currentThread().getName() + " wastesites done");

            try {
                a_barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            wasteSiteList = start_Cluster.getWasteSiteListCopy();

            if(calculateMedian()){
                Parallel.flag.set(true);
            }

            try {
                m_barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            end_barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean calculateMedian(){
        boolean change = false;
        if(!start_Cluster.getWasteSiteListCopy().isEmpty()) {

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
