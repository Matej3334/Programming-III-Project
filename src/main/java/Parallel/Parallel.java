package Parallel;

import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.JSONReader;
import org.example.WasteSite;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Parallel {
    private final int sites;
    private final int clusters;
    private final JSONReader jsonReader;
    private List<WasteSite> wasteSiteList;
    private final List<Cluster> clusterList;
    private static final int processors = Runtime.getRuntime().availableProcessors();
    private final ExecutorService threadPool;
    public static AtomicBoolean flag;
    private final CyclicBarrier a_barrier;
    private final CyclicBarrier m_barrier;
    private final CyclicBarrier end_barrier;
    public static AtomicInteger iterations;

    public Parallel(int sites, int clusters, String file_path){
        this.sites = sites;
        this.clusters = clusters;
        this.clusterList = new ArrayList<>();
        this.jsonReader = new JSONReader();
        this.threadPool = Executors.newFixedThreadPool(processors);

        iterations = new AtomicInteger(0);
        this.a_barrier = new CyclicBarrier(clusters, this::changeFlag);
        this.m_barrier = new CyclicBarrier(clusters,this::addCount);
        this.end_barrier = new CyclicBarrier(clusters,this::printResults);

        flag = new AtomicBoolean(true);
        InitializeWasteSites(file_path);
        InitializeClusters();
        Kmeans();
    }

    public void InitializeWasteSites(String file_path){
        //Getting an ArrayList of WasteSites from the file
        wasteSiteList = jsonReader.GetList(file_path);

        //Checking if the List is empty
        if(wasteSiteList.isEmpty()){
            System.out.println("Can't get waste site data");
            System.exit(1);
        }

        int siteNum = wasteSiteList.size(); //Getting the size of the List

        if(clusters > sites){
            System.out.println("Number of clusters cannot be larger than number of sites");
            System.exit(1);
        }

        if (sites > siteNum){ //If the number of sites that was requested is larger than dataset, create random sites
            int newSites = sites - siteNum;
            Random random = new Random();
            for (int i = 0; i < newSites; i++) {
                double capacity = random.nextDouble(1,180);
                double la = random.nextDouble(47.8, 53.2);
                double lo = random.nextDouble(5.9, 14.2);
                wasteSiteList.add(new WasteSite(capacity, la, lo));
            }
        } else if (sites < siteNum) { //if the number of sites is smaller than dataset, create a smaller ArrayList
            this.wasteSiteList = new ArrayList<>(wasteSiteList.subList(0, sites));
        }
    }

    public void InitializeClusters(){
        Random random = new Random();
        Set<Integer> startClusters = new HashSet<>();

        int ListSize = wasteSiteList.size();
        //Creating a set of random indexes
        while (startClusters.size() < clusters){
            startClusters.add(random.nextInt(ListSize));
        }

        //Assigning random WasteSites as Clusters in ClusterList
        for (Integer index : startClusters){
            WasteSite cluster = wasteSiteList.get(index);
            clusterList.add(new Cluster(cluster.la(),cluster.lo()));
        }
    }

    private void Kmeans(){
        for(WasteSite wasteSite : wasteSiteList){
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
            nearestCluster.addWasteSite(wasteSite);
        }

        for (int i = 0; i < clusterList.size(); i++) {
            FindNearestCluster worker = new FindNearestCluster(clusterList.get(i), clusterList, a_barrier, m_barrier, end_barrier);
            threadPool.submit(worker);
        }
    }
    public void printResults() {
        System.out.println("\n===== K-means Clustering Results =====");
        System.out.println("Number of clusters: " + clusterList.size());
        System.out.println("Number of waste sites: " + wasteSiteList.size());
        int i = 1;
        for(Cluster cluster: clusterList){
            System.out.println("Cluster " + i + " has " + cluster.getWasteSiteList().size() + " WasteSites.");
            i++;
        }
    }

    private void addCount(){
        iterations.incrementAndGet();
    }

    private void changeFlag(){
        flag.set(false);
    }

    public List<Cluster> getClusterList() {
        return clusterList;
    }
}
