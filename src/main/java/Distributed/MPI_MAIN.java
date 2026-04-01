package Distributed;


import mpi.MPI;
import org.example.*;


import java.io.*;
import java.util.*;


public class MPI_MAIN {
    private static final String file_path = "src/main/resources/germany/germany.json";
    private static final int width = 800;
    private static final int height = 600;
    private static List<Cluster> clusterList;
    private static List<WasteSite> wasteSiteList;
    private static List<Cluster> additonalClusters;

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
        int useGUI = Integer.parseInt(args[args.length-1]);

        int rank = MPI.COMM_WORLD.Rank();
        System.out.println(rank);
        int size = MPI.COMM_WORLD.Size();
        final int root = 0;

        List<Cluster> sendBuffer = null;
        int[] send = new int[1];
        long mpi_start = System.currentTimeMillis();
        if (rank == root) {
            //initialize everything and assign Waste sites to Clusters on Main Computer
            wasteSiteList = initializeWasteSiteList(sites, clusters);
            clusterList = initializeClusters(wasteSiteList, clusters);
            assignToClusters(wasteSiteList,clusterList);

            int remainder = clusters % size;

            if (remainder == 0) { // Divide the Clusters so that every computer gets an equal amount of Clusters
                sendBuffer=clusterList;
            } else {
                sendBuffer = clusterList.subList(0, clusterList.size() - remainder);
                additonalClusters = clusterList.subList(clusterList.size() - remainder, clusterList.size());
            }

            send[0] = (int) Math.floor(clusterList.size()/size);
        }

        boolean change = true;
        int iterations = 1;
        int cluster_size = 0;

        MPI.COMM_WORLD.Bcast(send,0,1,MPI.INT,0); //Broadcast the number of clusters everyone will receive

        cluster_size = send[0]; //get the size

        while(iterations < 20) { //Main loop
            //Define both buffers
            Cluster[] receiveBuffer = new Cluster[cluster_size];
            Cluster[] clusterArray = new Cluster[cluster_size*size];

            if(rank==0){ //get cluster array for sending
                clusterArray = sendBuffer.toArray(new Cluster[0]);
            }

            //Scatter so every Computer gets a piece of the ClusterArray
            MPI.COMM_WORLD.Scatter(clusterArray, 0, cluster_size, MPI.OBJECT,
                    receiveBuffer, 0, cluster_size, MPI.OBJECT, root);

            //Received Array and calculate the Mean of the Clusters in the array
            List<Cluster> rootClusters = Arrays.asList(receiveBuffer);
            calculateMean(rootClusters);

            Cluster[] rootArray = rootClusters.toArray(new Cluster[0]); //New clusters with updated Means
            Cluster[] gatherBuffer = new Cluster[cluster_size*size]; //Gather Buffer

            //Main Computer Gathers all the elements from every other rank
            MPI.COMM_WORLD.Gather(rootArray , 0, cluster_size, MPI.OBJECT,
                    gatherBuffer, 0, cluster_size, MPI.OBJECT, 0);

            if(rank == root) { //Main rank calculates
                List<Cluster> newClusterList = new ArrayList<>();
                newClusterList.addAll(Arrays.asList(gatherBuffer)); //Add all clusters to the same list

                if(additonalClusters!=null) { //These are leftover Clusters because there might be a remainder
                    calculateMean(additonalClusters);
                    newClusterList.addAll(additonalClusters); //Add them all to the New List
                }


                for (int i = 0; i < clusterList.size(); i++) { // If there was a change in the mean then change the coordinates of the original clusters
                    double distance = EuclideanDistance.calculate(
                            clusterList.get(i).getLa(), clusterList.get(i).getLo(),
                            newClusterList.get(i).getLa(), newClusterList.get(i).getLo());

                    if (distance > 0.001) {
                        clusterList.get(i).setLa(newClusterList.get(i).getLa());
                        clusterList.get(i).setLo(newClusterList.get(i).getLo());
                    }
                }

                assignToClusters(wasteSiteList, clusterList); //Reassign the wasteSites
            }

            iterations++;
        }


        if(rank==root) {
            printResults();
            if(useGUI==1) {
                GUI gui = new GUI(width, height);
                gui.drawClusters(clusterList);
            }
            long mpi_end = System.currentTimeMillis();
            System.out.println("Time spent computing: " + (mpi_end-mpi_start) + "ms");
        }

        System.out.println("FINISHED " + rank);
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

    public static void printResults() {
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