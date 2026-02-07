package org.example;

import java.util.*;

public class Sequential {
    private final int sites;
    private final int clusters;
    private final JSONReader jsonReader;
    private List<WasteSite> wasteSiteList;
    private final List<Cluster> clusterList;

    public Sequential(int sites, int clusters, String file_path){
        this.sites = sites;
        this.clusters = clusters;
        this.clusterList = new ArrayList<>();
        this.jsonReader = new JSONReader();
        InitializeWasteSites(file_path);
        InitializeClusters();
        Kmeans();
        printResults();
    }

    private void InitializeWasteSites(String file_path){
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
            Random random = new Random(2);
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

    private void InitializeClusters(){
        Random random = new Random(1);
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
        int iterationsWithNoChange = 0;
        int iterations = 0;

        while (iterationsWithNoChange <= 2 && iterations < 20) {
            boolean change = false;
            iterations++;

            for(Cluster cluster : clusterList){
                cluster.clearWasteSiteList();
            }

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

            for(Cluster cluster : clusterList){
                if(!cluster.getWasteSiteList().isEmpty()) {

                    double[] newCenter = cluster.changeCenter();

                    double distance = EuclideanDistance.calculate(
                            cluster.getLa(), cluster.getLo(), newCenter[0], newCenter[1]);

                    if (distance > 0.01) {
                        cluster.setLa(newCenter[0]);
                        cluster.setLo(newCenter[1]);
                        change = true;
                    }
                }
            }

            if(!change){
                iterationsWithNoChange += 1;
            }
            else{
                iterationsWithNoChange = 0;
            }
        }
        System.out.println("Kmeans finished with " + iterations + " iterations.");
    }

    public List<Cluster> getClusterList() {
        return clusterList;
    }
    private void printResults() {
        System.out.println("\n===== K-means Clustering Results =====");
        System.out.println("Number of clusters: " + clusterList.size());
        System.out.println("Number of waste sites: " + wasteSiteList.size());
        int i = 1;
        for(Cluster cluster: clusterList){
            System.out.println("Cluster " + i + " has " + cluster.getWasteSiteList().size() + " WasteSites.");
            i++;
        }
    }
}
