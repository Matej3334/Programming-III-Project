package Distributed;


import mpi.MPI;
import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.JSONReader;
import org.example.WasteSite;


import java.io.*;
import java.util.*;


public class MPI_MAIN {
    private static final String file_path = "src/main/resources/germany/germany.json";
    private static final int width = 800;
    private static final int height = 600;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if(args.length <4){
            System.out.println("Need 4 arguments to continue: The MPI.Size, " +
                    "number of waste sites, number of clusters and do you want GUI (1 for yes, 0 for no)");
            System.exit(1);
        }
        MPI.Init(args);

        int sites = Integer.parseInt(args[args.length-3]);
        System.out.println("site"+sites);
        int clusters = Integer.parseInt(args[args.length-2]);
        System.out.println("cluster"+clusters);
        //int useGUI = Integer.parseInt(args[args.length-1]);

        int rank = MPI.COMM_WORLD.Rank();
        System.out.println(rank);
        int size = MPI.COMM_WORLD.Size();
        final int root = 0;

        if (rank == root) {

            List<WasteSite> wasteSiteList = initializeWasteSiteList(sites, clusters);
            List<Cluster> clusterList = initializeClusters(wasteSiteList, clusters);
            assignToClusters(wasteSiteList,clusterList);
            runRoot(wasteSiteList, clusterList, sites, clusters, size);
        }
        else{
            runWorker(rank,size);
        }

        MPI.Finalize();
    }

    private static List<WasteSite> initializeWasteSiteList(int sites, int clusters){
        JSONReader jsonReader = new JSONReader();
        List<WasteSite> wasteSiteList = jsonReader.GetList(file_path);

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
            wasteSiteList = new ArrayList<>(wasteSiteList.subList(0, sites));
        }

        return wasteSiteList;
    }


    public static List<Cluster> initializeClusters(List<WasteSite> wasteSiteList, int clusters){
        List<Cluster> clusterList = new ArrayList<>();
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

        return clusterList;
    }


    private static void runRoot(List<WasteSite> wasteSiteList, List<Cluster> clusterList, int sites, int clusters, int size) throws IOException, ClassNotFoundException {
        assignToClusters(wasteSiteList,clusterList);

        boolean change = true;
        int iterations = 1;

        Object[] sendBuffer = new Object[size];

        for (int i = 0; i < size; i++) {
            sendBuffer[i] = new ArrayList<>();
        }

        int chunksize = (int) Math.floor(clusterList.size()/size);
        int remainder = clusterList.size() % size;

        int index = 0;
        for (int i = 0; i < clusterList.size(); i++) {
            int processSize = chunksize;

            if(i<remainder){
                processSize++;
            }
            for (int j = 0; j < processSize; j++) {
                ((List<Cluster>) sendBuffer[i]).add(clusterList.get(index));
                index++;
            }
        }

        while(change && iterations < 20) {
            change = false;

            Object[] receiveBuffer = new Object[1];

            MPI.COMM_WORLD.Scatter(sendBuffer, 0, 1, MPI.OBJECT,
                    receiveBuffer, 0, 1, MPI.OBJECT, 0);


            List<Cluster> rootClusters = (List<Cluster>) receiveBuffer[0];
            calculateMean(rootClusters);


            Object[] gatherBuffer = new Object[size];

            MPI.COMM_WORLD.Gather( rootClusters , 0, 1, MPI.OBJECT,
                    gatherBuffer, 0, 1, MPI.OBJECT, 0);



            List<Cluster> newClusterList = new ArrayList<>();

            for (int i = 0; i < gatherBuffer.length; i++) {
                if(gatherBuffer[i] != null){
                    newClusterList.addAll((Collection<? extends Cluster>) gatherBuffer[i]);
                }
            }

            for (int i = 0; i < clusterList.size(); i++) {
                double distance = EuclideanDistance.calculate(
                        clusterList.get(i).getLa(), clusterList.get(i).getLo(),
                        newClusterList.get(i).getLa(), newClusterList.get(i).getLo()) ;

                if (distance > 0.001) {
                    clusterList.get(i).setLa(newClusterList.get(i).getLa());
                    clusterList.get(i).setLo(newClusterList.get(i).getLo());
                    change = true;
                }
            }

            assignToClusters(wasteSiteList,clusterList);
            iterations++;
        }

        MPI.Finalize();
    }

    private static void runWorker(int rank, int size) throws IOException, ClassNotFoundException {

        while (true) {

            Object[] receiveBuffer = new Object[1];

            MPI.COMM_WORLD.Scatter(null, 0, 1, MPI.OBJECT,
                    receiveBuffer, 0, 1, MPI.OBJECT, 0);



            List<Cluster> clusterList = (List<Cluster>) receiveBuffer[0];

            if (clusterList == null || clusterList.isEmpty()) {
                break;
            }

            calculateMean(clusterList);


            MPI.COMM_WORLD.Gather(clusterList, 0, 1, MPI.OBJECT,
                    null, 0, 1, MPI.OBJECT, 0);
        }

        System.out.println("Worker with rank " + rank + " finished");
    }


    private static void assignToClusters(List<WasteSite> wasteSiteList, List<Cluster> clusterList){
        for(Cluster cluster: clusterList){
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
    }

    private static void calculateMean(List<Cluster> clusterList){

        for(Cluster cluster : clusterList){
            if(!cluster.getWasteSiteList().isEmpty()) {

                double[] newCenter = cluster.changeCenter();

                double distance = EuclideanDistance.calculate(
                        cluster.getLa(), cluster.getLo(), newCenter[0], newCenter[1]);

                if (distance > 0.001) {
                    cluster.setLa(newCenter[0]);
                    cluster.setLo(newCenter[1]);
                }
            }
        }
    }
}